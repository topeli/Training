package org.example;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;

public class Main {
    public static void main(String[] args) {

        System.out.println("Hello world!");
        System.out.println();
        Student student = new Student(2, "Gennadiy", 5);
        //System.out.println(student);
        //student.setId(33);
        System.out.println(student);
        Student student2 = new Student(2, "Gennadiy", 5);
        System.out.println(student2);
        if (student.equals(student2)) {
            System.out.println("Одинаковые");
        } else System.out.println("Разные");
        for (int i = 0; i < 11; i++) {
            System.out.print(i + " ");
        }
        int[] massive = new int[50];
        for (int i = 1; i < 51; i++) {
            massive[i-1] = i;
        }
        System.out.println(Arrays.toString(massive));
        ArrayList<Integer> list = new ArrayList<>();
        for (int i = 1; i < 100; i++) {
            list.add(i);
        }
        System.out.println(list);
        for (int i = 7; i <= 17; i++) {
            System.out.print(list.get(i-1) + " - ");
        }
        HashSet<String> set = new HashSet<>();
        set.add("Hello world!");
        System.out.println("Hi!");
        System.out.println();
        System.out.println(set);
        System.out.println("Hello students!!!");
    }
}