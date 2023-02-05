package org.example;

import java.util.Objects;

public class Cat {
    private String name;
    private int age;
    public Cat(){
        name = "котяг";
        age = 4;
    }
    public Cat(String name, int age){
        this.name = name;
        this.age = age;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    @Override
    public boolean equals(Object o) {
        Cat cat = (Cat) o;
        return this.age == cat.age && this.name.compareTo(cat.name)==0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, age);
    }
    public void Meow(){
        System.out.println("meow ฅ^•ﻌ•^ฅ");
    }
}
