# Books API

This project implements a simple REST API for books and authors.
It is implemented in Scala on the Play framework and backed by an H2 in-memory MySql database.
It is just for funsies, this is just a first pass, and there are plenty of things that wouldn't work for a production system, but I'd never worked with the Play framework before and I wanted to take a shot at writing some Scala webcode. 

I'd like to refactor some of the code to make everything cleaner and more modular if I have time, but for a side-project this gets the job done.

## Production Considerations
* The in-memory MySql H2 db works for the scale at which we're operating, but because we're doing text searches across author names, book titles, and book descriptions, a relational db is probably not the right tool for this job.  Book descriptions can be rather (5000+ characters) long, so storing these in something like ElasticSearch and creating indices on the author name and book title fields would greatly speed up search times.
* I wrote a raw SQL query for the search endpoint, because I was having trouble getting double joins in slick to work.  This is unacceptable for a production system, because the sql input isn't sanitizes and Timmy Droptables (https://xkcd.com/327/) might come around and destroy the db.
* There's some refactoring to be done to make the app more organized. I was writing this while figuring out how the Play Framework works, and some of how the app is currently structured is silly.
* There is really no front-end code to speak of.  It's not necessary for an api, but It would be nice for debugging.




## Dependencies and Setup
Setup directions provided are for system running Mac OS
### Scala
    brew install scala
### sbt
    brew install sbt
### ~~Python -- for bulk-post queries to populate the database from the given csv file.~~
#### No longer necessary, as I've added a csv bulk-loader endpoint to load local files.
    easy-install pip
    pip install virtualenv
    virtualenv <YOUR_ENV>
    source <YOUR_ENV>/bin/activate
    cd <PROJECT_DIR> && pip install -r requirements.txt

~~Python ships with Mac OS, but pip does not.  In order to build a virtual environment to isolate the library installs, (at this point only the requests library) you'll want pip.~~

### Running the project
Just cd into the root of the project and type
    sbt run
and you're good to go!  The app should be run on localhost port 9000.

### Supported Queries

#### The API Supports the following queries:
* GET     /books/
 Returns a json-list of all of the books, with descriptions, in the database
* POST 	/books/add/                        controllers.BooksController.addBook
 Takes a request with the following parameters and creates corresponding book, author and bookAuthor rows in their respective tables:
```{ 'authors': ['author1', 'author_2',..], 'title': 'book title', 'description': 'book description'}```
* **GET**     */authors/*
 Returns a json-list of all of the authors in the database

* **POST**    */authors/*
 Takes a request with the following parameters and creates a row in the Author table:
```{ 'name': 'authorname'}```
* **GET**    */books/search/:searchStr*            controllers.BooksController.searchBooks(searchStr)
Returns a json-list of all books such that the the book title, description, or author name is a case-sensitive match to the provided searchStr

* **POST**   */books/update/*
 Takes the same parameters as /books/add/, but instead of creating a new book, it updates the description of the book matching the provided title.
* **POST**   */authors/update/*
 Takes a request with the following parameters and changes the name of the requested author, without breaking the existing associations to any of that author's books.  I'm not entirely sure if this is what you wanted for this endpoint, but we can talk about it and discuss other implementations.
```{ 'oldName': 'name to replace', 'newName': 'name to change to'}```
* **GET** */bulkload/:fname*
 Convenience method for bulk-loading csv files into the database.  Takes a filename and loads all books from the file located at public/$fname.


