package com.smj10j.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;


@Retention(RetentionPolicy.RUNTIME)
public @interface MySQLTable {
  String name();
  String primaryKey();
  String[] transients();
}
