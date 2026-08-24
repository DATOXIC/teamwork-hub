// FILE NÀY GIẢI QUYẾT VẤN ĐỀ CƠ CHẾ XÁC THƯC
// NÊN CẦN PHẢI BIẾT USER SẼ CÓ NHỮNG THÔNG TIN GÌ

package com.teamwork.business;
import java.io.Serializable;

/**
 * Serializable là đóng gói đối tượng Java đang chạy trên RAM thành chuỗi Byte thô để CẤT VÀO Ổ CỨNG hoặc GỬI QUA MẠNG. Khi cần dùng lại, chỉ việc giải nén chuỗi Byte đó ngược lại thành đối tượng trên RAM.
 */
public class User implements Serializable {
    //thuộc tính
    private int id;
    private String username;
    private String password;
    private String fullName;
    private String email;
    private String role;
    private String avatar;

    public User()
    {
        this.id = 0;
        this.username = "";
        this.password = "";
        this.fullName = "";
        this.email = "";
        this.role = "MEMBER";
        this.avatar = "";
    }

    public User(int id, String username, String password, String fullName, String email, String role, String avatar) 
    {
        this.id = id;
        this.username = username;
        this.password = password;
        this.fullName = fullName;
        this.email = email;
        this.role = role;
        this.avatar = avatar;
    }

    public int getId() 
    {
        return id;
    }
    public void setId(int id) 
    {
        this.id = id;
    }
    public String getUsername() 
    {
        return username;
    }
    public void setUsername(String username) 
    {
        this.username = username;
    }
    public String getPassword() 
    {
        return password;
    }
    public void setPassword(String password) 
    {
        this.password = password;
    }
    public String getFullName() {
        return fullName;
    }
    public void setFullName(String fullName) {
        this.fullName = fullName;
    }
    public String getRole() {
        return role;
    }
    public void setRole(String role) {
        this.role = role;
    }
    public String getAvatar() 
    {
        return avatar;
    }
    public void setAvatar(String avatar) 
    {
        this.avatar = avatar;
    }
        public String getEmail() 
        {
        return email;
    }
    public void setEmail(String email) 
    {
        this.email = email;
    }

}

