# Setup Instructions for Banana Harvest App

## Prerequisites
- Java 17 or higher
- Maven 3.6+
- Supabase account

## Quick Setup

### 1. Get Your Supabase Credentials

Go to your Supabase project dashboard:
https://supabase.com/dashboard/project/ggbhnloweuwrbbyktuam/settings/api

You'll need:
- **Project URL**: `https://ggbhnloweuwrbbyktuam.supabase.co` (already configured)
- **anon/public key**: Copy this from the API settings
- **Database Password**: Your database password

### 2. Configure Environment Variables

Create a `.env` file in the project root (or set system environment variables):

```bash
# Database Password
DB_PASSWORD=your-actual-database-password

# Supabase Anon Key (get from dashboard)
SUPABASE_KEY=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...your-actual-key

# Optional: Change bucket name if needed
SUPABASE_STORAGE_BUCKET=banana-harvest
```

### 3. Create Storage Bucket in Supabase

1. Go to: https://supabase.com/dashboard/project/ggbhnloweuwrbbyktuam/storage/buckets
2. Click **New bucket**
3. Name: `banana-harvest`
4. Make it **Public** for direct URL access
5. Click **Create bucket**

### 4. Set Up Storage Policies

Go to Storage > Policies and add these:

```sql
-- Allow public read access
CREATE POLICY "Public Access"
ON storage.objects FOR SELECT
USING ( bucket_id = 'banana-harvest' );

-- Allow authenticated users to upload
CREATE POLICY "Authenticated users can upload"
ON storage.objects FOR INSERT
WITH CHECK (
  bucket_id = 'banana-harvest' 
  AND auth.role() = 'authenticated'
);

-- Allow users to delete their own files
CREATE POLICY "Users can delete own files"
ON storage.objects FOR DELETE
USING (
  bucket_id = 'banana-harvest'
  AND auth.role() = 'authenticated'
);
```

### 5. Update Database Schema (if needed)

The app will auto-create tables with `spring.jpa.hibernate.ddl-auto=update`.

If you want to use a specific schema:
```sql
CREATE SCHEMA IF NOT EXISTS banana_harvest;
```

### 6. Run the Application

Using Maven:
```bash
mvn spring-boot:run
```

Or with environment variables:
```bash
DB_PASSWORD=your-password SUPABASE_KEY=your-key mvn spring-boot:run
```

### 7. Verify Setup

1. Check if app started: http://localhost:8080/actuator/health
2. View API docs: http://localhost:8080/swagger-ui.html
3. Test file upload endpoint: POST http://localhost:8080/api/upload/photo

## Configuration Details

### Database Connection
```
Host: db.ggbhnloweuwrbbyktuam.supabase.co
Port: 5432
Database: postgres
Schema: banana_harvest
```

### Supabase Storage
```
Storage URL: https://ggbhnloweuwrbbyktuam.storage.supabase.co
Bucket: banana-harvest
```

### File Upload Limits
- Max file size: 10MB
- Max request size: 50MB
- Allowed image types: JPEG, JPG, PNG
- Allowed video types: MP4, QuickTime

## Testing File Upload

Using curl:
```bash
curl -X POST http://localhost:8080/api/upload/photo \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -F "file=@/path/to/photo.jpg"
```

Response:
```json
{
  "success": true,
  "message": "Photo uploaded successfully",
  "data": "https://ggbhnloweuwrbbyktuam.supabase.co/storage/v1/object/public/banana-harvest/photos/..."
}
```

## Troubleshooting

### Database Connection Issues
- Verify your database password
- Check if your IP is allowed in Supabase (Settings > Database > Connection Pooling)
- Ensure port 5432 is not blocked by firewall

### Storage Upload Issues
- Verify SUPABASE_KEY is correct (anon/public key)
- Check bucket exists and is public
- Verify storage policies are set up
- Check file size limits

### Authentication Issues
- Ensure JWT secret is set
- Check user is active in database
- Verify role-based permissions

## Production Deployment

For production, set these environment variables:

```bash
# Database
export DB_PASSWORD=your-production-password

# Supabase
export SUPABASE_KEY=your-production-anon-key
export SUPABASE_STORAGE_BUCKET=banana-harvest

# JWT (use a strong secret)
export JWT_SECRET=your-very-long-and-secure-secret-key-here

# Server
export PORT=8080

# JPA
export JPA_DDL_AUTO=validate  # Don't auto-update schema in production

# CORS
export CORS_ORIGINS=https://your-frontend-domain.com
```

## Support

- Supabase Docs: https://supabase.com/docs
- Spring Boot Docs: https://spring.io/projects/spring-boot
- API Documentation: http://localhost:8080/swagger-ui.html
