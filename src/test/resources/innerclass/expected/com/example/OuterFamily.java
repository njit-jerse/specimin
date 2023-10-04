package com.example;

class OuterFamily extends Maternal {

    class InnerFamily extends Paternal {

        public String getLastName() {
            return paternalLastName;
        }
    }

    public String getLastName() {
        return maternalLastName;
    }
}
