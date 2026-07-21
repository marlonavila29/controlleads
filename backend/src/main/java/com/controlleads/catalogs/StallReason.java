package com.controlleads.catalogs;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "stall_reasons")
public class StallReason extends CatalogItem {

    protected StallReason() {
    }

    public StallReason(String name) {
        super(name);
    }
}
