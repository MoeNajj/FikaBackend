-- ===============================================
-- Sample User Data (Optional - for testing)
-- ===============================================
-- WARNING: This is for development/testing only!
-- Delete this file or don't run it in production
-- ===============================================

USE BikeParkingDB;
GO

-- ===============================================
-- Insert Sample Users
-- ===============================================
-- Note: These passwords are hashed with BCrypt
-- Default password for both: "Password123!"
-- 
-- To generate your own BCrypt hash, use:
-- BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
-- String hash = encoder.encode("your-password");
-- ===============================================

-- Sample User 1: Regular User
IF NOT EXISTS (SELECT 1 FROM BikeParking.Users WHERE Username = 'testuser')
BEGIN
    INSERT INTO BikeParking.Users (Username, Email, PasswordHash, FirstName, LastName, IsActive, IsEmailVerified)
    VALUES (
        'testuser',
        'testuser@example.com',
        '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', -- Password123!
        'Test',
        'User',
        1,
        1
    );
    
    -- Assign USER role
    DECLARE @UserId INT = SCOPE_IDENTITY();
    DECLARE @UserRoleId INT = (SELECT RoleID FROM BikeParking.Roles WHERE RoleName = 'USER');
    
    INSERT INTO BikeParking.UserRoles (UserID, RoleID)
    VALUES (@UserId, @UserRoleId);
    
    PRINT 'Sample user ''testuser'' created with USER role.';
END
ELSE
BEGIN
    PRINT 'User ''testuser'' already exists.';
END
GO

-- Sample User 2: Admin User
IF NOT EXISTS (SELECT 1 FROM BikeParking.Users WHERE Username = 'admin')
BEGIN
    INSERT INTO BikeParking.Users (Username, Email, PasswordHash, FirstName, LastName, IsActive, IsEmailVerified)
    VALUES (
        'admin',
        'admin@bikeparking.com',
        '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', -- Password123!
        'Admin',
        'User',
        1,
        1
    );
    
    -- Assign ADMIN role
    DECLARE @AdminId INT = SCOPE_IDENTITY();
    DECLARE @AdminRoleId INT = (SELECT RoleID FROM BikeParking.Roles WHERE RoleName = 'ADMIN');
    DECLARE @UserRoleId2 INT = (SELECT RoleID FROM BikeParking.Roles WHERE RoleName = 'USER');
    
    INSERT INTO BikeParking.UserRoles (UserID, RoleID)
    VALUES (@AdminId, @AdminRoleId), (@AdminId, @UserRoleId2);
    
    PRINT 'Sample admin user ''admin'' created with ADMIN and USER roles.';
END
ELSE
BEGIN
    PRINT 'User ''admin'' already exists.';
END
GO

PRINT '===============================================';
PRINT 'Sample users created!';
PRINT 'Default password for both: Password123!';
PRINT '===============================================';
GO

