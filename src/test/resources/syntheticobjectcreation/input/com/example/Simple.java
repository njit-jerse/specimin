package com.example;
import org.not.a.real.pack.Writer;
import org.can.not.be.real.Pen;
import org.still.not.real.Ink;

public class Simple {
    private void createAWriter() {
        Ink blackInk = new Ink("black");
        Pen ironPen = new Pen(blackInk);
        Writer poet = new Writer(ironPen);
    }
}
