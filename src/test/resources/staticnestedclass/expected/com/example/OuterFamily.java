package com.example;

class OuterFamily extends Maternal {

    static class InnerFamily extends Paternal {

        public String getLastName() {
            return paternalLastName;
        }
    }

    public String getLastName() {
        return maternalLastName;
    }
}
