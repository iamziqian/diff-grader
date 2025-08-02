import React, { useState, useRef, useCallback } from 'react';
import { Card, Button, Alert, ProgressBar, Row, Col } from 'react-bootstrap';
import { UploadProgress, UploadedFile } from '../types';
import apiService from '../services/api';

interface FileUploadProps {
  onFileUploaded: (file: UploadedFile, type: 'student' | 'reference') => void;
  onError: (error: string) => void;
}

const FileUpload: React.FC<FileUploadProps> = ({ onFileUploaded, onError }) => {
  const [studentFile, setStudentFile] = useState<File | null>(null);
  const [referenceFile, setReferenceFile] = useState<File | null>(null);
  const [studentProgress, setStudentProgress] = useState<UploadProgress | null>(null);
  const [referenceProgress, setReferenceProgress] = useState<UploadProgress | null>(null);
  const [uploading, setUploading] = useState<boolean>(false);
  const [dragOver, setDragOver] = useState<{ student: boolean; reference: boolean }>({
    student: false,
    reference: false,
  });

  const studentFileRef = useRef<HTMLInputElement>(null);
  const referenceFileRef = useRef<HTMLInputElement>(null);

  const validateFile = (file: File): string | null => {
    const maxSize = 50 * 1024 * 1024; // 50MB
    const allowedTypes = ['application/zip', 'application/x-zip-compressed'];

    if (!allowedTypes.includes(file.type) && !file.name.toLowerCase().endsWith('.zip')) {
      return 'Only ZIP files are allowed';
    }

    if (file.size > maxSize) {
      return 'File size must be less than 50MB';
    }

    return null;
  };

  const handleFileSelect = (file: File, type: 'student' | 'reference') => {
    const error = validateFile(file);
    if (error) {
      onError(error);
      return;
    }

    if (type === 'student') {
      setStudentFile(file);
    } else {
      setReferenceFile(file);
    }
  };

     const handleDrop = useCallback(
     (e: React.DragEvent<HTMLDivElement>, type: 'student' | 'reference') => {
       e.preventDefault();
       setDragOver({ ...dragOver, [type]: false });

       const files = Array.from(e.dataTransfer.files);
       if (files.length > 0) {
         handleFileSelect(files[0], type);
       }
     },
     // eslint-disable-next-line react-hooks/exhaustive-deps
     [dragOver]
   );

  const handleDragOver = (e: React.DragEvent<HTMLDivElement>, type: 'student' | 'reference') => {
    e.preventDefault();
    setDragOver({ ...dragOver, [type]: true });
  };

  const handleDragLeave = (e: React.DragEvent<HTMLDivElement>, type: 'student' | 'reference') => {
    e.preventDefault();
    setDragOver({ ...dragOver, [type]: false });
  };

  const uploadFile = async (file: File, type: 'student' | 'reference') => {
    try {
      const response = await apiService.uploadFile(
        file,
        type,
        (progress: UploadProgress) => {
          if (type === 'student') {
            setStudentProgress(progress);
          } else {
            setReferenceProgress(progress);
          }
        }
      );

      if (response.success) {
        const uploadedFile: UploadedFile = {
          id: response.data.fileId,
          name: file.name,
          size: file.size,
          type,
          status: 'uploaded',
          uploadDate: new Date(),
        };
        onFileUploaded(uploadedFile, type);
      } else {
        throw new Error(response.error || 'Upload failed');
      }
    } catch (error) {
      onError(`Failed to upload ${type} file: ${error instanceof Error ? error.message : 'Unknown error'}`);
    }
  };

  const handleUpload = async () => {
    if (!studentFile || !referenceFile) {
      onError('Please select both student and reference files');
      return;
    }

    setUploading(true);
    setStudentProgress(null);
    setReferenceProgress(null);

    try {
      await Promise.all([
        uploadFile(studentFile, 'student'),
        uploadFile(referenceFile, 'reference'),
      ]);
    } catch (error) {
      onError(`Upload failed: ${error instanceof Error ? error.message : 'Unknown error'}`);
    } finally {
      setUploading(false);
      setStudentProgress(null);
      setReferenceProgress(null);
    }
  };

  const formatFileSize = (bytes: number): string => {
    if (bytes === 0) return '0 Bytes';
    const k = 1024;
    const sizes = ['Bytes', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
  };

  return (
    <Card className="mb-4">
      <Card.Header>
        <h4 className="mb-0">📁 Upload Assignment Files</h4>
      </Card.Header>
      <Card.Body>
        <Row>
          <Col md={6}>
            <div
              className={`upload-area p-4 border-2 border-dashed rounded text-center mb-3 ${
                dragOver.student ? 'border-primary bg-light' : 'border-secondary'
              }`}
              onDrop={(e) => handleDrop(e, 'student')}
              onDragOver={(e) => handleDragOver(e, 'student')}
              onDragLeave={(e) => handleDragLeave(e, 'student')}
              style={{ cursor: 'pointer' }}
              onClick={() => studentFileRef.current?.click()}
            >
              <div className="mb-2">
                <i className="fas fa-upload fa-2x text-primary"></i>
              </div>
              <h6>Student Assignment</h6>
              <p className="text-muted mb-2">
                Drag & drop ZIP file here or click to select
              </p>
              {studentFile && (
                <Alert variant="info" className="mt-2">
                  <strong>{studentFile.name}</strong><br />
                  Size: {formatFileSize(studentFile.size)}
                </Alert>
              )}
              {studentProgress && (
                <div className="mt-2">
                  <ProgressBar 
                    now={studentProgress.percentage} 
                    label={`${studentProgress.percentage}%`}
                    variant="primary"
                  />
                </div>
              )}
              <input
                ref={studentFileRef}
                type="file"
                accept=".zip"
                style={{ display: 'none' }}
                onChange={(e) => {
                  const file = e.target.files?.[0];
                  if (file) handleFileSelect(file, 'student');
                }}
              />
            </div>
          </Col>

          <Col md={6}>
            <div
              className={`upload-area p-4 border-2 border-dashed rounded text-center mb-3 ${
                dragOver.reference ? 'border-success bg-light' : 'border-secondary'
              }`}
              onDrop={(e) => handleDrop(e, 'reference')}
              onDragOver={(e) => handleDragOver(e, 'reference')}
              onDragLeave={(e) => handleDragLeave(e, 'reference')}
              style={{ cursor: 'pointer' }}
              onClick={() => referenceFileRef.current?.click()}
            >
              <div className="mb-2">
                <i className="fas fa-file-code fa-2x text-success"></i>
              </div>
              <h6>Reference Solution</h6>
              <p className="text-muted mb-2">
                Drag & drop ZIP file here or click to select
              </p>
              {referenceFile && (
                <Alert variant="success" className="mt-2">
                  <strong>{referenceFile.name}</strong><br />
                  Size: {formatFileSize(referenceFile.size)}
                </Alert>
              )}
              {referenceProgress && (
                <div className="mt-2">
                  <ProgressBar 
                    now={referenceProgress.percentage} 
                    label={`${referenceProgress.percentage}%`}
                    variant="success"
                  />
                </div>
              )}
              <input
                ref={referenceFileRef}
                type="file"
                accept=".zip"
                style={{ display: 'none' }}
                onChange={(e) => {
                  const file = e.target.files?.[0];
                  if (file) handleFileSelect(file, 'reference');
                }}
              />
            </div>
          </Col>
        </Row>

        <div className="text-center mt-3">
          <Button
            variant="primary"
            size="lg"
            onClick={handleUpload}
            disabled={!studentFile || !referenceFile || uploading}
          >
            {uploading ? (
              <>
                <span className="spinner-border spinner-border-sm me-2" role="status" aria-hidden="true"></span>
                Uploading...
              </>
            ) : (
              '🚀 Start Analysis'
            )}
          </Button>
        </div>

        <div className="mt-3">
          <small className="text-muted">
            <strong>Requirements:</strong>
            <ul className="mb-0 mt-1">
              <li>Only ZIP files are supported</li>
              <li>Maximum file size: 50MB</li>
              <li>ZIP should contain Java source code files (.java)</li>
            </ul>
          </small>
        </div>
      </Card.Body>
    </Card>
  );
};

export default FileUpload; 