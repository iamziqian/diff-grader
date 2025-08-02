import React, { useState, useEffect } from 'react';
import { Card, Row, Col, Nav, Badge, ListGroup, Alert } from 'react-bootstrap';
import { Editor } from '@monaco-editor/react';
import { ComparisonResult, CodeElement, ComparisonMatch } from '../types';

interface CodeComparisonProps {
  comparison: ComparisonResult;
  onElementSelect: (element: CodeElement, match?: ComparisonMatch) => void;
  selectedElementId?: string;
}

const CodeComparison: React.FC<CodeComparisonProps> = ({
  comparison,
  onElementSelect,
  selectedElementId,
}) => {
  const [activeTab, setActiveTab] = useState<'classes' | 'fields' | 'methods' | 'constructors'>('classes');
  const [selectedMatch, setSelectedMatch] = useState<ComparisonMatch | null>(null);

  useEffect(() => {
    // Auto-select first element when comparison loads
    if (comparison.matchedElements.length > 0) {
      const firstMatch = comparison.matchedElements[0];
      setSelectedMatch(firstMatch);
      onElementSelect(firstMatch.studentElement, firstMatch);
    }
  }, [comparison, onElementSelect]);

  const getElementsByType = (type: 'classes' | 'fields' | 'methods' | 'constructors') => {
    const matchedElements = comparison.matchedElements.filter(
      match => match.studentElement.type === type.slice(0, -1) as any
    );
    const unmatchedStudent = comparison.unmatchedStudentElements.filter(
      element => element.type === type.slice(0, -1) as any
    );
    const unmatchedReference = comparison.unmatchedReferenceElements.filter(
      element => element.type === type.slice(0, -1) as any
    );

    return { matchedElements, unmatchedStudent, unmatchedReference };
  };

  const getSimilarityColor = (similarity: number): string => {
    if (similarity >= 0.9) return 'success';
    if (similarity >= 0.7) return 'warning';
    return 'danger';
  };

  const getMatchTypeIcon = (matchType: string): string => {
    switch (matchType) {
      case 'exact': return '✅';
      case 'similar': return '≈';
      case 'missing': return '➖';
      case 'extra': return '➕';
      default: return '?';
    }
  };

  const handleElementClick = (element: CodeElement, match?: ComparisonMatch) => {
    setSelectedMatch(match || null);
    onElementSelect(element, match);
  };

  const renderElementList = () => {
    const { matchedElements, unmatchedStudent, unmatchedReference } = getElementsByType(activeTab);

    return (
      <ListGroup variant="flush" style={{ maxHeight: '400px', overflowY: 'auto' }}>
        {/* Matched elements */}
        {matchedElements.map((match) => (
          <ListGroup.Item
            key={match.studentElement.id}
            action
            active={selectedElementId === match.studentElement.id}
            onClick={() => handleElementClick(match.studentElement, match)}
            className="d-flex justify-content-between align-items-center"
          >
            <div>
              <div className="fw-bold">
                {getMatchTypeIcon('exact')} {match.studentElement.name}
              </div>
              <small className="text-muted">
                Line {match.studentElement.lineNumber}
              </small>
            </div>
            <Badge bg={getSimilarityColor(match.similarity)}>
              {Math.round(match.similarity * 100)}%
            </Badge>
          </ListGroup.Item>
        ))}

        {/* Unmatched student elements (extra) */}
        {unmatchedStudent.map((element) => (
          <ListGroup.Item
            key={element.id}
            action
            active={selectedElementId === element.id}
            onClick={() => handleElementClick(element)}
            className="d-flex justify-content-between align-items-center"
            variant="info"
          >
            <div>
              <div className="fw-bold">
                {getMatchTypeIcon('extra')} {element.name}
              </div>
              <small className="text-muted">
                Line {element.lineNumber} • Extra in student
              </small>
            </div>
            <Badge bg="info">Extra</Badge>
          </ListGroup.Item>
        ))}

        {/* Unmatched reference elements (missing) */}
        {unmatchedReference.map((element) => (
          <ListGroup.Item
            key={element.id}
            action
            variant="warning"
            className="d-flex justify-content-between align-items-center"
          >
            <div>
              <div className="fw-bold">
                {getMatchTypeIcon('missing')} {element.name}
              </div>
              <small className="text-muted">
                Line {element.lineNumber} • Missing in student
              </small>
            </div>
            <Badge bg="warning">Missing</Badge>
          </ListGroup.Item>
        ))}
      </ListGroup>
    );
  };

  return (
    <Card>
      <Card.Header>
        <div className="d-flex justify-content-between align-items-center">
          <h4 className="mb-0">🔍 Code Comparison</h4>
          <Badge bg="primary" pill>
            {Math.round(comparison.overallSimilarity * 100)}% Overall Similarity
          </Badge>
        </div>
      </Card.Header>
      <Card.Body>
        <Row>
          {/* Left sidebar - Element navigation */}
          <Col md={4}>
            <Nav variant="tabs" className="mb-3">
              <Nav.Item>
                <Nav.Link 
                  active={activeTab === 'classes'} 
                  onClick={() => setActiveTab('classes')}
                >
                  Classes
                </Nav.Link>
              </Nav.Item>
              <Nav.Item>
                <Nav.Link 
                  active={activeTab === 'fields'} 
                  onClick={() => setActiveTab('fields')}
                >
                  Fields
                </Nav.Link>
              </Nav.Item>
              <Nav.Item>
                <Nav.Link 
                  active={activeTab === 'methods'} 
                  onClick={() => setActiveTab('methods')}
                >
                  Methods
                </Nav.Link>
              </Nav.Item>
              <Nav.Item>
                <Nav.Link 
                  active={activeTab === 'constructors'} 
                  onClick={() => setActiveTab('constructors')}
                >
                  Constructors
                </Nav.Link>
              </Nav.Item>
            </Nav>

            {renderElementList()}
          </Col>

          {/* Right side - Code editors */}
          <Col md={8}>
            {selectedMatch ? (
              <>
                {selectedMatch.differences.length > 0 && (
                  <Alert variant="info" className="mb-3">
                    <strong>Differences found:</strong>
                    <ul className="mb-0 mt-1">
                      {selectedMatch.differences.map((diff, index) => (
                        <li key={index}>{diff}</li>
                      ))}
                    </ul>
                  </Alert>
                )}

                <Row>
                  <Col md={6}>
                    <h6 className="text-primary">Student Code</h6>
                    <Editor
                      height="400px"
                      language="java"
                      value={selectedMatch.studentElement.sourceCode}
                      options={{
                        readOnly: true,
                        minimap: { enabled: false },
                        scrollBeyondLastLine: false,
                        lineNumbers: 'on',
                        theme: 'vs-light',
                      }}
                    />
                  </Col>
                  <Col md={6}>
                    <h6 className="text-success">Reference Code</h6>
                    <Editor
                      height="400px"
                      language="java"
                      value={selectedMatch.referenceElement.sourceCode}
                      options={{
                        readOnly: true,
                        minimap: { enabled: false },
                        scrollBeyondLastLine: false,
                        lineNumbers: 'on',
                        theme: 'vs-light',
                      }}
                    />
                  </Col>
                </Row>
              </>
            ) : (
              <div className="text-center text-muted p-5">
                <i className="fas fa-code fa-3x mb-3"></i>
                <h5>Select an element to view comparison</h5>
                <p>Click on any code element from the left panel to see the side-by-side comparison</p>
              </div>
            )}
          </Col>
        </Row>
      </Card.Body>
    </Card>
  );
};

export default CodeComparison; 