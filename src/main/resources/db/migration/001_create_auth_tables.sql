-- ===============================================
-- Authentication Tables Migration
-- Database: BikeParkingDB
-- Schema: BikeParking
-- ===============================================

USE BikeParkingDB;
GO

-- ===============================================
-- 1. Users Table
-- Stores user account information
-- Passwords are HASHED (not encrypted) using BCrypt
-- ===============================================
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'Users' AND schema_id = SCHEMA_ID('BikeParking'))
BEGIN
    CREATE TABLE BikeParking.Users (
        UserID INT IDENTITY(1,1) PRIMARY KEY,
        Username NVARCHAR(100) NOT NULL UNIQUE,
        Email NVARCHAR(255) NOT NULL UNIQUE,
        PasswordHash NVARCHAR(255) NOT NULL, -- BCrypt hashed password (60 chars)
        FirstName NVARCHAR(100) NULL,
        LastName NVARCHAR(100) NULL,
        IsActive BIT NOT NULL DEFAULT 1,
        IsEmailVerified BIT NOT NULL DEFAULT 0,
        CreatedAt DATETIME2 NOT NULL DEFAULT GETUTCDATE(),
        UpdatedAt DATETIME2 NOT NULL DEFAULT GETUTCDATE(),
        LastLogin DATETIME2 NULL,
        
        -- Indexes for performance
        INDEX IX_Users_Username (Username),
        INDEX IX_Users_Email (Email),
        INDEX IX_Users_IsActive (IsActive)
    );
    
    PRINT 'Table BikeParking.Users created successfully.';
END
ELSE
BEGIN
    PRINT 'Table BikeParking.Users already exists.';
END
GO

-- ===============================================
-- 2. Roles Table
-- Stores available roles (USER, ADMIN, etc.)
-- ===============================================
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'Roles' AND schema_id = SCHEMA_ID('BikeParking'))
BEGIN
    CREATE TABLE BikeParking.Roles (
        RoleID INT IDENTITY(1,1) PRIMARY KEY,
        RoleName NVARCHAR(50) NOT NULL UNIQUE,
        Description NVARCHAR(255) NULL,
        CreatedAt DATETIME2 NOT NULL DEFAULT GETUTCDATE(),
        
        INDEX IX_Roles_RoleName (RoleName)
    );
    
    -- Insert default roles
    INSERT INTO BikeParking.Roles (RoleName, Description) VALUES
        ('USER', 'Standard user with basic permissions'),
        ('ADMIN', 'Administrator with full permissions');
    
    PRINT 'Table BikeParking.Roles created successfully with default roles.';
END
ELSE
BEGIN
    PRINT 'Table BikeParking.Roles already exists.';
END
GO

-- ===============================================
-- 3. UserRoles Table (Many-to-Many)
-- Links users to their roles
-- ===============================================
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'UserRoles' AND schema_id = SCHEMA_ID('BikeParking'))
BEGIN
    CREATE TABLE BikeParking.UserRoles (
        UserRoleID INT IDENTITY(1,1) PRIMARY KEY,
        UserID INT NOT NULL,
        RoleID INT NOT NULL,
        AssignedAt DATETIME2 NOT NULL DEFAULT GETUTCDATE(),
        
        -- Foreign Keys
        CONSTRAINT FK_UserRoles_Users FOREIGN KEY (UserID) 
            REFERENCES BikeParking.Users(UserID) ON DELETE CASCADE,
        CONSTRAINT FK_UserRoles_Roles FOREIGN KEY (RoleID) 
            REFERENCES BikeParking.Roles(RoleID) ON DELETE CASCADE,
        
        -- Unique constraint: user can only have each role once
        CONSTRAINT UQ_UserRoles_User_Role UNIQUE (UserID, RoleID),
        
        -- Indexes
        INDEX IX_UserRoles_UserID (UserID),
        INDEX IX_UserRoles_RoleID (RoleID)
    );
    
    PRINT 'Table BikeParking.UserRoles created successfully.';
END
ELSE
BEGIN
    PRINT 'Table BikeParking.UserRoles already exists.';
END
GO

-- ===============================================
-- 4. RefreshTokens Table
-- Stores refresh tokens for JWT authentication
-- ===============================================
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'RefreshTokens' AND schema_id = SCHEMA_ID('BikeParking'))
BEGIN
    CREATE TABLE BikeParking.RefreshTokens (
        TokenID BIGINT IDENTITY(1,1) PRIMARY KEY,
        UserID INT NOT NULL,
        Token NVARCHAR(500) NOT NULL UNIQUE, -- JWT refresh token
        ExpiresAt DATETIME2 NOT NULL,
        IsRevoked BIT NOT NULL DEFAULT 0,
        RevokedAt DATETIME2 NULL,
        CreatedAt DATETIME2 NOT NULL DEFAULT GETUTCDATE(),
        CreatedByIp NVARCHAR(45) NULL, -- Store IP address for security
        DeviceInfo NVARCHAR(255) NULL, -- Store device/user agent info
        
        -- Foreign Key
        CONSTRAINT FK_RefreshTokens_Users FOREIGN KEY (UserID) 
            REFERENCES BikeParking.Users(UserID) ON DELETE CASCADE,
        
        -- Indexes
        INDEX IX_RefreshTokens_UserID (UserID),
        INDEX IX_RefreshTokens_Token (Token),
        INDEX IX_RefreshTokens_ExpiresAt (ExpiresAt),
        INDEX IX_RefreshTokens_IsRevoked (IsRevoked)
    );
    
    PRINT 'Table BikeParking.RefreshTokens created successfully.';
END
ELSE
BEGIN
    PRINT 'Table BikeParking.RefreshTokens already exists.';
END
GO

-- ===============================================
-- 5. Optional: Link ParkingSpots.UserUUID to Users
-- This creates a foreign key relationship if you want
-- to link parking spots to user accounts
-- ===============================================
-- Note: Uncomment this if you want to enforce referential integrity
-- between ParkingSpots.UserUUID and Users table
/*
IF EXISTS (SELECT * FROM sys.tables WHERE name = 'ParkingSpots' AND schema_id = SCHEMA_ID('BikeParking'))
BEGIN
    -- First, we'd need to add a UserID column to ParkingSpots or create a mapping
    -- This is optional and depends on your requirements
    PRINT 'Consider adding UserID foreign key to ParkingSpots table if needed.';
END
GO
*/

-- ===============================================
-- Cleanup: Remove expired refresh tokens (Optional stored procedure)
-- ===============================================
IF EXISTS (SELECT * FROM sys.procedures WHERE name = 'sp_CleanupExpiredTokens' AND schema_id = SCHEMA_ID('BikeParking'))
BEGIN
    DROP PROCEDURE BikeParking.sp_CleanupExpiredTokens;
END
GO

CREATE PROCEDURE BikeParking.sp_CleanupExpiredTokens
AS
BEGIN
    SET NOCOUNT ON;
    
    -- Delete expired and revoked tokens older than 30 days
    DELETE FROM BikeParking.RefreshTokens
    WHERE (ExpiresAt < GETUTCDATE() OR IsRevoked = 1)
      AND CreatedAt < DATEADD(DAY, -30, GETUTCDATE());
    
    PRINT 'Expired refresh tokens cleaned up.';
END
GO

PRINT '===============================================';
PRINT 'Authentication tables migration completed!';
PRINT '===============================================';
GO

