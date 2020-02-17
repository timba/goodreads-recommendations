# Goodreads API Client in Clojure

## Intro

Books recommendation tool for Goodreads users follows the next simple algorithm:
for a given user find all books marked as “read” and choose top 10 by average rating from “similar”
books excluding books that user is currently reading. Uses “similarity” definition provided by
Goodreads API (each book has assigned list of "similar books").

## Async version

There exists asynchronous version implementation of this client. Please checkout branch `aleph-cli` for more details.

## Inconsistent rating information note

During implementation a bug was found in Goodreads API call `book/show` which returns similar books with incorrect 
ratings information. This issue has been reported on developers discussion board https://www.goodreads.com/topic/show/19969016-similar-books-rating-info-mismatch-in-book-show-response
Until this issue fixed, incorrect recommendations returned. There are few chances to avoid this by calling book info for each "similar book" recommendation because full data obtaining will require a tramendous amount of time.

## Links

* [API Documentation](https://www.goodreads.com/api/index)

* Rate limits [policy](https://www.goodreads.com/topic/show/17540788-what-s-rate-limit-of-your-api#comment_141992829)

* [List of books](https://www.goodreads.com/api/index#reviews.list)

* [Info on each book](https://www.goodreads.com/api/index#book.show) (find similar books and ratings)

## Usage

Run using `lein`:

```shell
    $ lein run <TOKEN> -k <API-KEY> [<OPTIONS>]
```

Where:

 - `<TOKEN>` is ID of Goodreads user. The ID could be found in user's profile URL: https://www.goodreads.com/user/show/XXX-user-name 
 Here, `XXX` is user ID.

 - `<API-KEY>` is API key to access Goodreads API. To get this key, one needs to be Googlereads user and follow this URL: https://www.goodreads.com/api/keys

 - `<OPTIONS>` optional config params.

 To see all params use this command:

```shell
    $ lein run -- -h
```

Please note that the program limits its execution by timeout which by default is 5000 ms. The GR API is relatively slow, and often the program breaks by timeout. Timeout could changed via optional `-t` parameter: `-t 30000` where time is in milliseconds.

Compile and run from JAR:

```shell
    $ lein uberjar
    $ java -jar target/uberjar/goodreads-0.1.0-SNAPSHOT-standalone.jar <TOKEN> -k <API-KEY> [<OPTIONS>]
```

JAR accepts all the same arguments as lein version does.