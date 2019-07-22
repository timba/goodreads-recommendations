(ns goodreads.core
  (:gen-class)
  (:require [clojure.tools.cli    :as cli]
            [manifold.deferred    :as d]
            [manifold.stream      :as s]
            [clojure.xml          :as cxml]
            [clojure.zip          :as zip]
            [clojure.data.zip.xml :as xml]
;            [aleph.http           :as http]
            [byte-streams         :as bs]
            [clj-http.client      :as http]
            [clojure.java.io      :as io]))

(def shelf-reading "currently-reading")
(def shelf-read    "read")

(defn read-xml-str [xml-str]
  (-> xml-str
      .getBytes
      io/input-stream
      cxml/parse
      zip/xml-zip))

(defn get-now []
  (. System (nanoTime)))

(defn get-diff-ms 
  "Gets duration in ms from two nanosecond points"
  [start end]
  (int (/ (- end start) 1000000)))

;Last time of HTTP call in system nanoseconds
(def last-call (ref (get-now)))

(defn create-throttler [limit-ms target-fn]
  (fn [& target-args]
    (dosync
     (let [now (get-now)
           last (deref last-call)
           diff-ms (get-diff-ms last now)]
       (if (< diff-ms limit-ms)
         (Thread/sleep (- limit-ms diff-ms)))
       (ref-set last-call (get-now))))
    (apply target-fn target-args)))

(defn create-caller [api-key]
  (fn [path params]
    (let [start (get-now)
          response (http/get (str "https://www.goodreads.com/" path)
                             {:query-params (assoc params :key api-key)
                              :cookie-policy :none})
          result (-> response
                          :body
                          read-xml-str)
          end (get-now)
          duration (get-diff-ms start end)]
;      (println "call duration:" duration "ms")
      result)))

(defn create-throttled-caller [api-key limit-ms]
  (create-throttler limit-ms 
                   (create-caller api-key)))

(defn get-read-status [shelves]
  (cond
    (some #{shelf-read} shelves) :read 
    (some #{shelf-reading} shelves) :reading
    :else :none))

(defn get-book-info-from-review [review]
  (hash-map
   :id          (xml/xml1-> review :book :id xml/text)
   :title       (xml/xml1-> review :book :title xml/text)
   :read-status (get-read-status 
                 (xml/xml-> review :shelves :shelf 
                            (xml/attr :name)))))

(defn get-user-books [caller user-id shelf per-page page] 
  (println (str "getting books from shelf " shelf ", page " page))
  (let [request-data {:id user-id
                      :v 2
                      :shelf shelf
                      :per_page per-page
                      :page page }

        reviews (xml/xml1-> 
                 (caller "review/list.xml" request-data) 
                 :reviews)]

    (hash-map
     :start (xml/xml1-> reviews (xml/attr :start))
     :end   (xml/xml1-> reviews (xml/attr :end))
     :total (xml/xml1-> reviews (xml/attr :total))
     :books (map get-book-info-from-review (xml/xml-> reviews :review)))))
    
(defn get-all-user-books 
  "Gets all books from shelf"
  [caller user-id shelf]

  (filter #(not= (:read-status %) :none) 
          (loop [page 1
                 all-books []]
            (let [response-books (get-user-books caller user-id shelf 50 page)
                  {end   :end
                   total :total
                   books :books} response-books
                  new-all-books (into all-books books)]
              (if (= end total)
                new-all-books
                (recur (inc page) new-all-books))))))

(defn get-author-info [author]
  (hash-map
   :name (xml/xml1-> author :name xml/text)))

(defn get-book-info [book]
  (hash-map
     :id             (xml/xml1-> book :id xml/text)
     :title          (xml/xml1-> book :title xml/text)
     :link           (xml/xml1-> book :link xml/text)
     :average-rating (read-string (xml/xml1-> book :average_rating xml/text))
     :ratings-count  (read-string (xml/xml1-> book :ratings_count xml/text))
     :authors        (map get-author-info (xml/xml-> book :authors :author))))

(defn get-book [caller book-id]
  (let [book (xml/xml1-> 
              (caller "book/show.xml" {:id book-id})
              :book)]
    (assoc (get-book-info book)
           :similar (map get-book-info (xml/xml-> book :similar_books :book)))))

(defn get-book-similarities [caller book-id]
  (println (str "getting book info: " book-id))
  (let [book (xml/xml1-> 
              (caller "book/show.xml" {:id book-id})
              :book)]
    (map get-book-info (xml/xml-> book :similar_books :book))))

(defn build-recommendations [api-key user-id number-books]
  (d/future
   (let [caller (create-throttled-caller api-key 1000)
         books-read (get-all-user-books caller user-id shelf-read)
         books-reading (get-all-user-books caller user-id shelf-reading)
         books-reading-ids (map #(:id %) books-reading)
                                        ;Here we use map {"book-id" {book}} to avoid
                                        ;duplications. We don't use set because such book info
                                        ;as average-rating and ratings-count could potentially
                                        ;change between API calls.
         books-similar (reduce (fn [similar book] 
                                 (into similar 
                                       (map (fn [similar-book] [(:id similar-book) similar-book]) 
                                            (get-book-similarities caller (:id book))))) 
                               {} books-read)]
     (println (str "total read: " (count books-read)))
     (println (str "total reading: " (count books-reading)))
     (println (str "total similar: " (count books-similar)))

     (take number-books
           (reverse
                                        ;Small improvement: sort by rating and ratings count.
            (sort-by (juxt :average-rating :ratings-count)
                     (filter (fn [book] (not  (some #{(:id book)} books-reading-ids))) 
                             (vals books-similar))))))))
(def cli-options [["-k"
                   "--api-key APIKEY"
                   "Goodreads developer API Key"
                   :required "Goodreads developer API Key"]
                  ["-t"
                   "--timeout-ms TIMEOUT"
                   "Wait before finished"
                   :default 5000
                   :parse-fn #(Integer/parseInt %)]
                  ["-n"
                   "--number-books NUMBER"
                   "How many books do you want to recommend"
                   :default 10
                   :parse-fn #(Integer/parseInt %)]
                  ["-h" "--help"]])

(defn book->str [{:keys [title link authors]}]
  (format "\"%s\" by %s\nMore: %s"
          title
          (->> authors
               (map :name)
               (clojure.string/join ", "))
          link))

(defn -main [& args]
  (let [{:keys [options errors summary]} (cli/parse-opts args cli-options)]
    (cond
      (contains? options :help) (do (println summary) (System/exit 0))
      (some? errors) (do (println errors) (System/exit 1))
      (empty? args) (do (println "Please specify user's token") (System/exit 1))
      (not (contains? options :api-key)) (do 
                                           (println "Please specify Goodreads developer API key") 
                                           (System/exit 1))
      :else (let [user-id (first args) 
                  api-key (:api-key options) 
                  number-books (:number-books options)
                  timeout-ms (:timeout-ms options)
                  books-future (build-recommendations api-key user-id number-books)
                  books (-> books-future
                            (d/timeout! timeout-ms ::timeout)
                            (deref)
                            ;(deref timeout-ms ::timeout)
                            )]
              (System/exit 
               (cond
                 (= ::timeout books) (do
                                       ;(future-cancel books-future)
                                       (println "Not enough time :(")
                                       1)
                 (empty? books) (do
                                    (println "Nothing found, leave me alone :(")
                                    0)
                 :else (do (doseq [[i book] (map-indexed vector books)]
                             (println (str "#" (inc i)))
                             (println (book->str book))
                             (println))
                           0)))))))
