# Goodreads API Client in Clojure

## Intro

Books recommendation tool for Goodreads users follows the next simple algorithm:
for a given user find all books marked as “read” and choose top 10 by average rating from “similar”
books excluding books that user is currently reading. Uses “similarity” definition provided by
Goodreads API (each book has assigned list of "similar books").

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

Compile and run from JAR:

```shell
    $ lein uberjar
    $ java -jar target/uberjar/goodreads-0.1.0-SNAPSHOT-standalone.jar <TOKEN> -k <API-KEY> [<OPTIONS>]
```

## License

Proprietary.

Copyright © 2019