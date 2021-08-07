package com.example.passmanager.model;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import java.sql.Date;

/**
 * Entry in the Password Manager.
 *
 * <p>This class represents an entry which stores the authentication credentials and other information
 *    about a service (an app or a website).
 * </p>
 */
@Entity(tableName = "Entries", foreignKeys = {@ForeignKey(entity = Category.class,
    parentColumns = "categoryId",
    childColumns = "categoryId",
    onDelete = ForeignKey.SET_DEFAULT)})
public class Entry {

    /**
     * Entry ID in the Password Manager.
     */
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "entryId")
    public int entryId;

    /**
     * Name of the entry, typically the name of the website/app.
     */
    @ColumnInfo(name = "name")
    public String name;

    /**
     * Description of the entry.
     */
    @ColumnInfo(name = "description")
    public String description;

    /**
     * Link/URL/URI of the service.
     */
    @ColumnInfo(name = "link")
    public String link;

    /**
     * User ID for authentication (it can be an e-mail or a username).
     */
    @ColumnInfo(name = "username")
    public String username;

    /**
     * User password for authentication, stored in an encrypted form by using a salt.
     */
    @ColumnInfo(name = "password")
    public String password;

    /**
     * Salt generated for encrypting/decrypting the user password.
     */
    @ColumnInfo(name = "passwordSalt")
    public String passwordSalt;

    /**
     * Date of last modification.
     */
    @ColumnInfo(name = "lastModified")
    public Date lastModified;

    /**
     * The default category of an entry is the first category in the database, which should be
     * the "Others" category. When a category is deleted, all of its entries are moved to this
     * category, with the help of the onDelete rule in the foreign key declaration at the
     * top of this class.
     */
    @ColumnInfo(name = "categoryId", defaultValue = "1", index = true)
    public int categoryId;

    public Entry(String name, String description, String link, String username, String password,
                 String passwordSalt, Date lastModified, int categoryId) {
        this.name = name;
        this.description = description;
        this.link = link;
        this.username = username;
        this.password = password;
        this.passwordSalt = passwordSalt;
        this.lastModified = lastModified;
        this.categoryId = categoryId;
    }
}
