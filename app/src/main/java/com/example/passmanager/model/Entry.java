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
    parentColumns = "categoryNo",
    childColumns = "categoryNo",
    onDelete = ForeignKey.SET_DEFAULT)})
public class Entry {

    /**
     * Entry number in the Password Manager.
     */
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "entryNo")
    public int entryNo;

    /**
     * Name of the entry, typically the name of the website/app.
     */
    @ColumnInfo(name = "entryName")
    public String entryName;

    /**
     * Description of the entry.
     */
    @ColumnInfo(name = "entryDescription")
    public String entryDescription;

    /**
     * Filesystem path to a picture associated with the entry.
     */
    @ColumnInfo(name = "entryPicPath")
    public String entryPicPath;

    /**
     * Link/URL/URI of the service.
     */
    @ColumnInfo(name = "serviceLink")
    public String serviceLink;

    /**
     * User ID for authentication (it can be an e-mail or a username).
     */
    @ColumnInfo(name = "userId")
    public String userId;

    /**
     * User password for authentication, stored in an encrypted form by using a salt.
     */
    @ColumnInfo(name = "userPassword")
    public String userPassword;

    /**
     * Salt generated for encrypting/decrypting the user password.
     */
    @ColumnInfo(name = "passwordSalt")
    public String passwordSalt;

    @ColumnInfo(name = "lastModified")
    public Date lastModified;

    /**
     * The default category of an entry is the first category in the database, which should be
     * the "Others" category. When a category is deleted, all of its entries are moved to this
     * category, with the help of the onDelete rule in the foreign key declaration at the
     * top of this class.
     */
    @ColumnInfo(name = "categoryNo", defaultValue = "1")
    public int categoryNo;

    public Entry(String entryName, String entryDescription, String entryPicPath,
                 String serviceLink, String userId, String userPassword, String passwordSalt,
                 Date lastModified, int categoryNo) {
        this.entryName = entryName;
        this.entryDescription = entryDescription;
        this.entryPicPath = entryPicPath;
        this.serviceLink = serviceLink;
        this.userId = userId;
        this.userPassword = userPassword;
        this.passwordSalt = passwordSalt;
        this.lastModified = lastModified;
        this.categoryNo = categoryNo;
    }

    @Ignore
    public Entry(int entryNo, String entryName, String entryDescription, String entryPicPath,
                 String serviceLink, String userId, String userPassword, String passwordSalt) {
        this.entryNo = entryNo;
        this.entryName = entryName;
        this.entryDescription = entryDescription;
        this.entryPicPath = entryPicPath;
        this.serviceLink = serviceLink;
        this.userId = userId;
        this.userPassword = userPassword;
        this.passwordSalt = passwordSalt;
    }
}
