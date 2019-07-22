# Goodreads API Client in Clojure

## Intro

Books recommendation tool for Goodreads users follows the next simple algorithm:
for a given user find all books marked as “read” and choose top 10 by average rating from “similar”
books excluding books that user is currently reading. Uses “similarity” definition provided by
Goodreads API (each book has assigned list of "similar books").

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