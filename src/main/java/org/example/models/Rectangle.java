package org.example.models;
import javax.persistence.Table;
@Table (name = "rectangle")
public class Rectangle {
     int a;

    public int getB() {
        return b;
    }

    public void setB(int b) {
        this.b = b;
    }

    int b;

    public int getA() {
        return a;
    }

    public void setA(int a) {
        this.a = a;
    }
}
