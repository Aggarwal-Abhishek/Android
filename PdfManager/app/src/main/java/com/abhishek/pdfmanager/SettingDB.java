package com.abhishek.pdfmanager;

import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;

/**
 * Created by Abhishek on 7/2/2018.
 */

@Entity
public class SettingDB {
    @Id public long id;
    String str;

    SettingDB(){}

    public SettingDB(long id, String str) {
        this.id = id;
        this.str = str;
    }
}
