package com.example;

import an.old.library.Book;

class Simple {
    // Target method.
    int test() {
        Book bookOfTheYear = new Book(2023);
        return bookOfTheYear.getRates().length();
    }
}
