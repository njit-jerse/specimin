package com.example;

record Rectangle(double len, double width) {

    public Rectange rotate() {
        return new Rectangle(this.width, this.len());
    }
}