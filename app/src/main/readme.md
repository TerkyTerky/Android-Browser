# Android浏览器

## 功能要求

* 浏览器基本功能：前进后退历史记录等；
* 云书签、收藏夹  

## 需求分析

* 前进
* 后退
* 历史记录
* 云书签
* 登陆界面，登陆结束的同时需要对书签进行云同步

## 功能设计

### 登录功能

因为需要做云书签，所以需要对特定对象进行处理

### 书签

使用Android本地的SQLite实现

#### 数据库设计

Table User:
id varchar(1024) PK
password(1024)

Table bookmark:
user_id varchar(1024) FK
bookmark_name varchar(1024)
bookmark_website varchar(1024)

### 前进

### 后退

### 历史记录

