-- ===============================================
-- Add IsParked Column to ParkingSpots Table
-- Database: BikeParkingDB
-- Schema: BikeParking
-- ===============================================

USE BikeParkingDB;
GO

-- ===============================================
-- Add IsParked column to ParkingSpots table
-- ===============================================
IF NOT EXISTS (
    SELECT * FROM sys.columns 
    WHERE object_id = OBJECT_ID('BikeParking.ParkingSpots') 
    AND name = 'IsParked'
)
BEGIN
    ALTER TABLE BikeParking.ParkingSpots
    ADD IsParked BIT NOT NULL DEFAULT 0;
    
    PRINT 'Column IsParked added to BikeParking.ParkingSpots table successfully.';
END
ELSE
BEGIN
    PRINT 'Column IsParked already exists in BikeParking.ParkingSpots table.';
END
GO

-- ===============================================
-- Add index for better query performance
-- ===============================================
IF NOT EXISTS (
    SELECT * FROM sys.indexes 
    WHERE name = 'IX_ParkingSpots_IsParked' 
    AND object_id = OBJECT_ID('BikeParking.ParkingSpots')
)
BEGIN


    CREATE INDEX IX_ParkingSpots_IsParked ON BikeParking.ParkingSpots(IsParked);
    PRINT 'Index IX_ParkingSpots_IsParked created successfully.';
END
ELSE
BEGIN
    PRINT 'Index IX_ParkingSpots_IsParked already exists.';
END
GO

PRINT '===============================================';
PRINT 'IsParked column migration completed!';
PRINT '===============================================';
GO

