package com.controlleads.catalogs;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "courses")
public class Course extends CatalogItem {

    protected Course() {
    }

    public Course(String name) {
        super(name);
    }
}
