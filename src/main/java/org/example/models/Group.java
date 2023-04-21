package org.example.models;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name="group_password")
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class Group {
    @Id
    private String group;

    private String password;
}
