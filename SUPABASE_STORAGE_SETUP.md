# Supabase Storage Setup Guide

This application now uses Supabase Storage instead of AWS S3 for storing photos and videos.

## Prerequisites

1. A Supabase project (create one at https://supabase.com)
2. Supabase project URL and API key

## Setup Steps

### 1. Create Storage Bucket in Supabase

1. Go to your Supabase project dashboard
2. Navigate to **Storage** in the left sidebar
3. Click **New bucket**
4. Create a bucket named `banana-harvest` (or your preferred name)
5. Set the bucket to **Public** if you want direct URL access, or **Private** for signed URLs

### 2. Configure Bucket Policies

For public bucket, add this policy:

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

### 3. Update Application Configuration

Update your `application.properties` or environment variables:

```properties
# Supabase Configuration
supabase.url=https://your-project-id.supabase.co
supabase.key=your-supabase-anon-key-here
supabase.storage.bucket=banana-harvest
```

Or set environment variables:

```bash
export SUPABASE_URL=https://your-project-id.supabase.co
export SUPABASE_KEY=your-supabase-anon-key
export SUPABASE_STORAGE_BUCKET=banana-harvest
```

### 4. Get Your Supabase Credentials

1. Go to **Project Settings** > **API**
2. Copy your **Project URL** (e.g., `https://xxxxx.supabase.co`)
3. Copy your **anon/public** key (this is safe to use in your backend)

## API Endpoints

The following endpoints are now available for file uploads:

### Upload Single Photo
```
POST /api/upload/photo
Content-Type: multipart/form-data

Parameters:
- file: MultipartFile (required)
- inspectionId: String (optional)

Response:
{
  "success": true,
  "message": "Photo uploaded successfully",
  "data": "https://your-project.supabase.co/storage/v1/object/public/banana-harvest/photos/..."
}
```

### Upload Multiple Photos
```
POST /api/upload/photos
Content-Type: multipart/form-data

Parameters:
- files: List<MultipartFile> (required)
- inspectionId: String (optional)
```

### Upload Video
```
POST /api/upload/video
Content-Type: multipart/form-data

Parameters:
- file: MultipartFile (required)
- inspectionId: String (optional)
```

### Upload Inspection Media
```
POST /api/upload/inspection-media
Content-Type: multipart/form-data

Parameters:
- photos: List<MultipartFile> (4-5 photos required)
- video: MultipartFile (required)
```

### Delete File
```
DELETE /api/upload/file?fileUrl=<url>
```

## File Storage Structure

Files are organized in the following structure:

```
banana-harvest/
├── photos/
│   └── {userId}_{timestamp}_{uniqueId}.jpg
└── videos/
    └── {userId}_{timestamp}_{uniqueId}.mp4
```

## Features

1. **Fraud Prevention**: Only camera-captured photos/videos are accepted
2. **Validation**: File type, size, and metadata validation
3. **Organized Storage**: Files organized by type and user
4. **Public URLs**: Direct access to uploaded files (if bucket is public)
5. **Signed URLs**: Temporary access URLs for private buckets

## Database Integration

The photo/video URLs returned by the upload endpoints should be stored in your PostgreSQL database columns:

```sql
-- Example: FarmPhoto table
CREATE TABLE farm_photos (
    id UUID PRIMARY KEY,
    inspection_id UUID REFERENCES farm_inspections(id),
    photo_url TEXT NOT NULL,  -- Store Supabase URL here
    created_at TIMESTAMP DEFAULT NOW()
);
```

## Migration from S3

If you're migrating from S3:

1. The old S3StorageService is now commented out
2. All references have been updated to use SupabaseStorageService
3. Update your database to point to new Supabase URLs
4. Optionally migrate existing files from S3 to Supabase

## Troubleshooting

### Upload fails with 401 Unauthorized
- Check that your `supabase.key` is correct
- Verify the key has proper permissions

### Upload fails with 403 Forbidden
- Check your bucket policies
- Ensure the bucket exists and is accessible

### Files not accessible
- If using a private bucket, use signed URLs
- Check CORS settings in Supabase dashboard

### Large file uploads fail
- Check `spring.servlet.multipart.max-file-size` setting
- Verify Supabase plan limits

## Security Best Practices

1. **Never expose your service_role key** - Use anon key in backend
2. **Implement Row Level Security (RLS)** on storage buckets
3. **Validate file types** before upload
4. **Set file size limits** appropriately
5. **Use signed URLs** for sensitive content
6. **Implement rate limiting** on upload endpoints

## Cost Optimization

1. Set up lifecycle policies to delete old files
2. Use image optimization/compression before upload
3. Monitor storage usage in Supabase dashboard
4. Consider CDN for frequently accessed files

## Support

For issues related to:
- Supabase Storage: https://supabase.com/docs/guides/storage
- Application: Check application logs
- API: Use Swagger UI at `/swagger-ui.html`
