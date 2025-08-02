import React, { useState, useEffect } from 'react';
import { Container, Alert, Row, Col, Spinner, Card } from 'react-bootstrap';
import 'bootstrap/dist/css/bootstrap.min.css';
import './App.css';

import Header from './components/Header';
import FileUpload from './components/FileUpload';
import CodeComparison from './components/CodeComparison';
import FeedbackPanel from './components/FeedbackPanel';
import { 
  GradingSession, 
  UploadedFile, 
  ComparisonResult, 
  CodeElement, 
  ComparisonMatch, 
  Feedback 
} from './types';
import apiService from './services/api';

interface AppState {
  session: GradingSession | null;
  comparison: ComparisonResult | null;
  selectedElement: CodeElement | null;
  selectedMatch: ComparisonMatch | null;
  feedbacks: Feedback[];
  loading: boolean;
  error: string | null;
  step: 'upload' | 'analyzing' | 'comparison' | 'completed';
}

const App: React.FC = () => {
  const [state, setState] = useState<AppState>({
    session: null,
    comparison: null,
    selectedElement: null,
    selectedMatch: null,
    feedbacks: [],
    loading: false,
    error: null,
    step: 'upload',
  });

     // Auto-refresh comparison status
   useEffect(() => {
     if (state.session && state.session.status === 'analyzing') {
       const interval = setInterval(async () => {
         try {
           const response = await apiService.getGradingSession(state.session!.id);
           if (response.success) {
             const updatedSession = response.data;
             setState(prev => ({ ...prev, session: updatedSession }));

             if (updatedSession.status === 'ready') {
               clearInterval(interval);
               await loadComparison(updatedSession.id);
             }
           }
         } catch (error) {
           console.error('Failed to check session status:', error);
         }
       }, 2000);

       return () => clearInterval(interval);
     }
     // eslint-disable-next-line react-hooks/exhaustive-deps
   }, [state.session]);

  const handleError = (error: string) => {
    setState(prev => ({ ...prev, error, loading: false }));
  };

  const clearError = () => {
    setState(prev => ({ ...prev, error: null }));
  };

  const handleFileUploaded = async (uploadedFiles: { student: UploadedFile; reference: UploadedFile }) => {
    setState(prev => ({ ...prev, loading: true, error: null }));

    try {
      const response = await apiService.createGradingSession(
        uploadedFiles.student.id,
        uploadedFiles.reference.id
      );

      if (response.success) {
        setState(prev => ({
          ...prev,
          session: response.data,
          loading: false,
          step: 'analyzing',
        }));
      } else {
        throw new Error(response.error || 'Failed to create grading session');
      }
    } catch (error) {
      handleError(`Failed to create grading session: ${error instanceof Error ? error.message : 'Unknown error'}`);
    }
  };

  const loadComparison = async (sessionId: string) => {
    setState(prev => ({ ...prev, loading: true }));

    try {
      const [comparisonResponse, feedbacksResponse] = await Promise.all([
        apiService.getComparison(sessionId),
        apiService.getFeedbacks(sessionId),
      ]);

      if (comparisonResponse.success) {
        setState(prev => ({
          ...prev,
          comparison: comparisonResponse.data,
          feedbacks: feedbacksResponse.success ? feedbacksResponse.data : [],
          loading: false,
          step: 'comparison',
        }));
      } else {
        throw new Error(comparisonResponse.error || 'Failed to load comparison');
      }
    } catch (error) {
      handleError(`Failed to load comparison: ${error instanceof Error ? error.message : 'Unknown error'}`);
    }
  };

  const handleElementSelect = (element: CodeElement, match?: ComparisonMatch) => {
    setState(prev => ({
      ...prev,
      selectedElement: element,
      selectedMatch: match || null,
    }));
  };

  const handleFeedbackSaved = (feedback: Feedback) => {
    setState(prev => ({
      ...prev,
      feedbacks: [
        ...prev.feedbacks.filter(f => f.id !== feedback.id),
        feedback,
      ],
    }));
  };

  const handleReset = () => {
    setState({
      session: null,
      comparison: null,
      selectedElement: null,
      selectedMatch: null,
      feedbacks: [],
      loading: false,
      error: null,
      step: 'upload',
    });
  };

  // Multi-file upload handler
  const [uploadedFiles, setUploadedFiles] = useState<{
    student?: UploadedFile;
    reference?: UploadedFile;
  }>({});

  const handleSingleFileUploaded = (file: UploadedFile, type: 'student' | 'reference') => {
    const newUploadedFiles = { ...uploadedFiles, [type]: file };
    setUploadedFiles(newUploadedFiles);

    // If both files are uploaded, create session
    if (newUploadedFiles.student && newUploadedFiles.reference) {
      handleFileUploaded(newUploadedFiles as { student: UploadedFile; reference: UploadedFile });
    }
  };

  const renderStepIndicator = () => {
    const steps = [
      { key: 'upload', label: 'Upload Files', icon: 'fas fa-upload' },
      { key: 'analyzing', label: 'Analyzing Code', icon: 'fas fa-cog fa-spin' },
      { key: 'comparison', label: 'View Comparison', icon: 'fas fa-balance-scale' },
      { key: 'completed', label: 'Completed', icon: 'fas fa-check-circle' },
    ];

    return (
      <Card className="mb-4">
        <Card.Body>
          <div className="d-flex justify-content-center">
            {steps.map((step, index) => (
              <div
                key={step.key}
                className={`d-flex align-items-center ${
                  index > 0 ? 'ms-4' : ''
                } ${
                  state.step === step.key ? 'text-primary fw-bold' : 
                  steps.findIndex(s => s.key === state.step) > index ? 'text-success' : 'text-muted'
                }`}
              >
                <i className={`${step.icon} me-2`}></i>
                <span>{step.label}</span>
                {index < steps.length - 1 && (
                  <i className="fas fa-chevron-right ms-4 text-muted"></i>
                )}
              </div>
            ))}
          </div>
        </Card.Body>
      </Card>
    );
  };

  return (
    <div className="App">
      <Header overallSimilarity={state.comparison?.overallSimilarity} />
      
      <Container>
        {renderStepIndicator()}

        {state.error && (
          <Alert variant="danger" dismissible onClose={clearError} className="mb-4">
            <Alert.Heading>Error</Alert.Heading>
            {state.error}
          </Alert>
        )}

        {state.loading && (
          <div className="text-center mb-4">
            <Spinner animation="border" variant="primary" />
            <p className="mt-2">Processing...</p>
          </div>
        )}

        {state.step === 'upload' && (
          <FileUpload
            onFileUploaded={handleSingleFileUploaded}
            onError={handleError}
          />
        )}

                 {state.step === 'analyzing' && state.session && (
           <Card>
             <Card.Body className="text-center py-5">
               <Spinner animation="border" variant="primary" style={{ width: '3rem', height: '3rem' }} />
               <h4 className="mt-3">Analyzing Code Structure</h4>
               <p className="text-muted">
                 Please wait while we extract and compare code elements...
               </p>
               <div className="mt-4">
                 <small className="text-muted">
                   <strong>Student File:</strong> {state.session.studentFile.name}<br />
                   <strong>Reference File:</strong> {state.session.referenceFile.name}
                 </small>
               </div>
             </Card.Body>
           </Card>
         )}

        {state.step === 'comparison' && state.comparison && (
          <>
            <Row>
              <Col>
                <CodeComparison
                  comparison={state.comparison}
                  onElementSelect={handleElementSelect}
                  selectedElementId={state.selectedElement?.id}
                />
              </Col>
            </Row>

            <Row className="mt-4">
              <Col>
                                 <FeedbackPanel
                   sessionId={state.session!.id}
                   selectedElement={state.selectedElement || undefined}
                   selectedMatch={state.selectedMatch || undefined}
                   onFeedbackSaved={handleFeedbackSaved}
                   onError={handleError}
                 />
              </Col>
            </Row>

            <Row className="mt-4">
              <Col className="text-center">
                <button 
                  className="btn btn-outline-secondary me-3"
                  onClick={handleReset}
                >
                  <i className="fas fa-redo me-2"></i>
                  Start New Analysis
                </button>
                <button 
                  className="btn btn-success"
                  onClick={() => setState(prev => ({ ...prev, step: 'completed' }))}
                  disabled={state.feedbacks.length === 0}
                >
                  <i className="fas fa-check me-2"></i>
                  Complete Grading
                </button>
              </Col>
            </Row>
          </>
        )}

        {state.step === 'completed' && (
          <Card>
            <Card.Body className="text-center py-5">
              <i className="fas fa-check-circle fa-4x text-success mb-4"></i>
              <h3>Grading Completed!</h3>
              <p className="text-muted mb-4">
                You have successfully completed the grading process.
              </p>
              <div className="mb-4">
                <strong>Summary:</strong>
                <ul className="list-unstyled mt-2">
                  <li>Total Feedback Items: {state.feedbacks.length}</li>
                  <li>Overall Similarity: {state.comparison ? Math.round(state.comparison.overallSimilarity * 100) : 0}%</li>
                </ul>
              </div>
              <button 
                className="btn btn-primary"
                onClick={handleReset}
              >
                <i className="fas fa-plus me-2"></i>
                Start New Grading Session
              </button>
            </Card.Body>
          </Card>
        )}
      </Container>
    </div>
  );
};

export default App; 