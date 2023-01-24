package org.example;

public class Dog
{
    private int age;
    private String name;
    private int numberOfCommands;
    public Dog(int age, String name, int commands)
    {
        this.age = age;
        this.name = name;
        this.numberOfCommands = commands;
    }
    public Dog()
    {
        this.age = 2;
        this.name = "Vlad";
        this.numberOfCommands = 5;
    }
    public String Name(){
        return this.name;
    }
    public int getAge(){
        return this.age;
    }
    public int getCommands()
    {
        return this.numberOfCommands;
    }
    public void setCommands(int c)
    {
        this.numberOfCommands = c;
    }
    public String Bark()
    {
        return "Gav!";
    }
    public void Learn()
    {
        this.numberOfCommands += 1;
    }
    @Override
    public boolean equals(Object o) {
        Dog dog = (Dog) o;
        return this.numberOfCommands == dog.numberOfCommands && this.name.compareTo(dog.name)==0 && this.age == dog.age;
    }
}
