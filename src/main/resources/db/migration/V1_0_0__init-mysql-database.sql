
    drop table if exists beer;

    drop table if exists customer;

    create table beer (
       id varchar(36) not null,
        beer_name varchar(50) not null,
        beer_style smallint not null,
        created_date datetime(6),
        price decimal(38,2) not null,
        quantity_on_hand integer,
        upc varchar(255) not null,
        update_date datetime(6),
        version integer,
        primary key (id)
    ) engine=InnoDB;
/*
    CUIDADO con la sintaxis de las sentencias si tenemos cosas no estándar acopladas al Vendero - MySql
    En este caso * engine=InnoDB *

    Si lanzamos las pruebas Flyway intentará crear la estructura en la bbdd en memoria H2 y dara el siguiente error

    Caused by: org.springframework.beans.factory.BeanCreationException: Error creating bean with name 'flywayInitializer' defined in class path resource [org/springframework/boot/autoconfigure/flyway/FlywayAutoConfiguration$FlywayConfiguration.class]: Migration V1_0_0__init-mysql-database.sql failed
    ------------------------------------------------
    SQL State  : 42001
    Error Code : 42001
    Message    : Error de Sintaxis en sentencia SQL "create table beer (\000d\000a       id varchar(36) not null,\000d\000a        beer_name varchar(50) not null,\000d\000a        beer_style smallint not null,\000d\000a        created_date datetime(6),\000d\000a        price decimal(38,2) not null,\000d\000a        quantity_on_hand integer,\000d\000a        upc varchar(255) not null,\000d\000a        update_date datetime(6),\000d\000a        version integer,\000d\000a        primary key (id)\000d\000a    ) engine[*]=InnoDB"; se esperaba "identifier"
    Syntax error in SQL statement "create table beer (\000d\000a       id varchar(36) not null,\000d\000a        beer_name varchar(50) not null,\000d\000a        beer_style smallint not null,\000d\000a        created_date datetime(6),\000d\000a        price decimal(38,2) not null,\000d\000a        quantity_on_hand integer,\000d\000a        upc varchar(255) not null,\000d\000a        update_date datetime(6),\000d\000a        version integer,\000d\000a        primary key (id)\000d\000a    ) engine[*]=InnoDB"; expected "identifier"; SQL statement:
    create table beer (

    DOS POSIBLES SOLUCIONES

    1 - Deshabilitamos FlyWay para las pruebas en el fichero de configuración y dejamos que sea Hibernate el que cree el Schema

    2 - Utilizar FlyWay con scripts específicos para cada vendor
*/

    create table customer (
       id varchar(36) not null,
        created_date datetime(6),
        name varchar(255),
        update_date datetime(6),
        version integer,
        primary key (id)
    ) engine=InnoDB;

/*
    CUIDADO con la inclusión dentro de estos scripts ya que modifican el CHECKSUM y no coincide con la migración apicada a la BBDD
    ERROR
    rg.springframework.beans.factory.BeanCreationException: Error creating bean with name 'flywayInitializer' defined in class path resource [org/springframework/boot/autoconfigure/flyway/FlywayAutoConfiguration$FlywayConfiguration.class]: Validate failed: Migrations have failed validation
    Migration checksum mismatch for migration version 1.0.0
    -> Applied to database : -1219805925
    -> Resolved locally    : -19964026
    Either revert the changes to the migration, or run repair to update the schema history.

    SOLUCIÓN -> No se pueden modificar los scrips aplicados a la bbdd, todo trabajo tiene que ser incremental en otro fichero

 */