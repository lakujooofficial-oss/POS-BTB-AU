-- Supabase Database Setup for POS-BTB-AU
-- Run these SQL commands in your Supabase SQL Editor

-- Create products table
CREATE TABLE IF NOT EXISTS products (
    id SERIAL PRIMARY KEY,
    name TEXT NOT NULL,
    price DECIMAL(10,2) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Create promotions table
CREATE TABLE IF NOT EXISTS promotions (
    id SERIAL PRIMARY KEY,
    name TEXT NOT NULL,
    discount_percent DECIMAL(5,2) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Create sales table
CREATE TABLE IF NOT EXISTS sales (
    id SERIAL PRIMARY KEY,
    time TIMESTAMP WITH TIME ZONE NOT NULL,
    total DECIMAL(10,2) NOT NULL,
    receipt TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Enable Row Level Security (RLS) for security
ALTER TABLE products ENABLE ROW LEVEL SECURITY;
ALTER TABLE promotions ENABLE ROW LEVEL SECURITY;
ALTER TABLE sales ENABLE ROW LEVEL SECURITY;

-- Create policies to allow anonymous access (for demo purposes)
-- In production, use proper authentication
CREATE POLICY "Allow all operations on products" ON products FOR ALL USING (true);
CREATE POLICY "Allow all operations on promotions" ON promotions FOR ALL USING (true);
CREATE POLICY "Allow all operations on sales" ON sales FOR ALL USING (true);

-- Optional: Create indexes for better performance
CREATE INDEX IF NOT EXISTS idx_products_name ON products(name);
CREATE INDEX IF NOT EXISTS idx_sales_time ON sales(time);
CREATE INDEX IF NOT EXISTS idx_promotions_name ON promotions(name);