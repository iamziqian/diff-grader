import React, { useState, useEffect } from 'react';
import { Card, Form, Button, Row, Col, Alert, Badge } from 'react-bootstrap';
import { Feedback, CodeElement, ComparisonMatch } from '../types';
import apiService from '../services/api';

interface FeedbackPanelProps {
  sessionId: string;
  selectedElement?: CodeElement;
  selectedMatch?: ComparisonMatch;
  onFeedbackSaved: (feedback: Feedback) => void;
  onError: (error: string) => void;
}

const FeedbackPanel: React.FC<FeedbackPanelProps> = ({
  sessionId,
  selectedElement,
  selectedMatch,
  onFeedbackSaved,
  onError,
}) => {
  const [score, setScore] = useState<number>(75);
  const [comments, setComments] = useState<string>('');
  const [designPatternFeedback, setDesignPatternFeedback] = useState<string>('');
  const [bestPracticesFeedback, setBestPracticesFeedback] = useState<string>('');
  const [saving, setSaving] = useState<boolean>(false);
  const [existingFeedback, setExistingFeedback] = useState<Feedback | null>(null);

     useEffect(() => {
     if (selectedElement) {
       loadExistingFeedback();
     }
     // eslint-disable-next-line react-hooks/exhaustive-deps
   }, [selectedElement, sessionId]);

     const loadExistingFeedback = async () => {
     if (!selectedElement) return;

     try {
       const response = await apiService.getFeedbacks(sessionId);
       if (response.success) {
         const feedback = response.data.find(f => f.elementId === selectedElement!.id);
         if (feedback) {
           setExistingFeedback(feedback);
           setScore(feedback.score);
           setComments(feedback.comments);
           setDesignPatternFeedback(feedback.designPatternFeedback);
           setBestPracticesFeedback(feedback.bestPracticesFeedback);
         } else {
           resetForm();
         }
       }
     } catch (error) {
       console.error('Failed to load existing feedback:', error);
     }
   };

  const resetForm = () => {
    setExistingFeedback(null);
    setScore(75);
    setComments('');
    setDesignPatternFeedback('');
    setBestPracticesFeedback('');
  };

  const getScoreColor = (score: number): string => {
    if (score >= 85) return 'success';
    if (score >= 70) return 'warning';
    return 'danger';
  };

  const getScoreLabel = (score: number): string => {
    if (score >= 90) return 'Excellent';
    if (score >= 80) return 'Good';
    if (score >= 70) return 'Satisfactory';
    if (score >= 60) return 'Needs Improvement';
    return 'Poor';
  };

  const handleSave = async () => {
    if (!selectedElement) {
      onError('No element selected');
      return;
    }

    setSaving(true);

    try {
             const feedbackData = {
         comparisonId: sessionId,
         elementId: selectedElement!.id,
         score,
         comments: comments.trim(),
         designPatternFeedback: designPatternFeedback.trim(),
         bestPracticesFeedback: bestPracticesFeedback.trim(),
       };

      let response;
      if (existingFeedback) {
        response = await apiService.updateFeedback(sessionId, existingFeedback.id, feedbackData);
      } else {
        response = await apiService.submitFeedback(sessionId, feedbackData);
      }

      if (response.success) {
        setExistingFeedback(response.data);
        onFeedbackSaved(response.data);
      } else {
        throw new Error(response.error || 'Failed to save feedback');
      }
    } catch (error) {
      onError(`Failed to save feedback: ${error instanceof Error ? error.message : 'Unknown error'}`);
    } finally {
      setSaving(false);
    }
  };

  const suggestFeedback = () => {
    if (!selectedMatch) return;

    const similarity = selectedMatch.similarity;
    const differences = selectedMatch.differences;

    // Auto-suggest score based on similarity
    const suggestedScore = Math.round(similarity * 100);
    setScore(suggestedScore);

    // Auto-suggest comments based on differences
    if (differences.length > 0) {
      const suggestionMap: { [key: string]: string } = {
        'method signature': 'Consider reviewing method signatures for consistency with requirements.',
        'naming convention': 'Pay attention to Java naming conventions (camelCase for methods/variables, PascalCase for classes).',
        'access modifier': 'Review access modifiers - ensure proper encapsulation principles.',
        'parameter': 'Check method parameters - ensure they match the expected interface.',
        'return type': 'Verify return types match the specification.',
        'exception handling': 'Consider adding proper exception handling.',
        'comments': 'Add meaningful comments to explain complex logic.',
      };

      const suggestions = differences.map(diff => {
        const key = Object.keys(suggestionMap).find(k => diff.toLowerCase().includes(k));
        return key ? suggestionMap[key] : `Review: ${diff}`;
      });

      setComments(suggestions.join('\n\n'));
    }

         // Suggest design pattern feedback
     if (selectedElement && selectedElement.type === 'class') {
       setDesignPatternFeedback('Consider applying relevant design patterns like Singleton, Factory, or Strategy pattern where appropriate.');
     } else if (selectedElement && selectedElement.type === 'method') {
       setDesignPatternFeedback('Ensure methods follow Single Responsibility Principle and are properly abstracted.');
     }

    // Suggest best practices feedback
    setBestPracticesFeedback('Follow Java coding standards: proper indentation, meaningful variable names, and appropriate use of access modifiers.');
  };

  if (!selectedElement) {
    return (
      <Card>
        <Card.Header>
          <h4 className="mb-0">📝 Feedback Panel</h4>
        </Card.Header>
        <Card.Body>
          <div className="text-center text-muted p-4">
            <i className="fas fa-comment-alt fa-3x mb-3"></i>
            <h5>Select an element to provide feedback</h5>
            <p>Choose a code element from the comparison view to start grading</p>
          </div>
        </Card.Body>
      </Card>
    );
  }

  return (
    <Card>
      <Card.Header>
        <div className="d-flex justify-content-between align-items-center">
          <h4 className="mb-0">📝 Feedback Panel</h4>
          {existingFeedback && (
            <Badge bg="info">Previously Graded</Badge>
          )}
        </div>
      </Card.Header>
      <Card.Body>
                 <Alert variant="light" className="mb-3">
           <strong>Grading:</strong> {selectedElement!.name} 
           <span className="text-muted"> ({selectedElement!.type})</span>
           {selectedMatch && (
             <div className="mt-1">
               <small>
                 Similarity: <Badge bg={getScoreColor(selectedMatch.similarity * 100)}>
                   {Math.round(selectedMatch.similarity * 100)}%
                 </Badge>
               </small>
             </div>
           )}
         </Alert>

        <Form>
          <Row>
            <Col md={6}>
              <Form.Group className="mb-3">
                <Form.Label>
                  Score: <strong>{score}/100</strong> 
                  <Badge bg={getScoreColor(score)} className="ms-2">
                    {getScoreLabel(score)}
                  </Badge>
                </Form.Label>
                <Form.Range
                  value={score}
                  onChange={(e) => setScore(parseInt(e.target.value))}
                  min={0}
                  max={100}
                  step={5}
                />
                <div className="d-flex justify-content-between text-muted small">
                  <span>0</span>
                  <span>50</span>
                  <span>100</span>
                </div>
              </Form.Group>
            </Col>
            <Col md={6}>
              <div className="d-flex align-items-end h-100 pb-3">
                <Button 
                  variant="outline-primary" 
                  size="sm" 
                  onClick={suggestFeedback}
                  disabled={!selectedMatch}
                >
                  🤖 Auto-suggest Feedback
                </Button>
              </div>
            </Col>
          </Row>

          <Form.Group className="mb-3">
            <Form.Label>General Comments</Form.Label>
            <Form.Control
              as="textarea"
              rows={3}
              value={comments}
              onChange={(e) => setComments(e.target.value)}
              placeholder="Provide specific feedback about this code element..."
            />
          </Form.Group>

          <Form.Group className="mb-3">
            <Form.Label>Design Pattern Feedback</Form.Label>
            <Form.Control
              as="textarea"
              rows={2}
              value={designPatternFeedback}
              onChange={(e) => setDesignPatternFeedback(e.target.value)}
              placeholder="Comment on design patterns usage, SOLID principles, etc..."
            />
          </Form.Group>

          <Form.Group className="mb-3">
            <Form.Label>Best Practices Feedback</Form.Label>
            <Form.Control
              as="textarea"
              rows={2}
              value={bestPracticesFeedback}
              onChange={(e) => setBestPracticesFeedback(e.target.value)}
              placeholder="Comment on coding standards, naming conventions, etc..."
            />
          </Form.Group>

          <div className="d-flex justify-content-end">
            <Button
              variant="primary"
              onClick={handleSave}
              disabled={saving || (!comments.trim() && !designPatternFeedback.trim() && !bestPracticesFeedback.trim())}
            >
              {saving ? (
                <>
                  <span className="spinner-border spinner-border-sm me-2" role="status" aria-hidden="true"></span>
                  Saving...
                </>
              ) : existingFeedback ? (
                '💾 Update Feedback'
              ) : (
                '💾 Save Feedback'
              )}
            </Button>
          </div>
        </Form>
      </Card.Body>
    </Card>
  );
};

export default FeedbackPanel; 