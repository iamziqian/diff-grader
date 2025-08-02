import React from 'react';
import { Navbar, Nav, Container, Badge } from 'react-bootstrap';

interface HeaderProps {
  overallSimilarity?: number;
}

const Header: React.FC<HeaderProps> = ({ overallSimilarity }) => {
  return (
    <Navbar bg="dark" variant="dark" expand="lg" className="mb-4">
      <Container>
        <Navbar.Brand href="#home" className="fw-bold">
          <i className="fas fa-code-branch me-2"></i>
          DiffGrader
          <small className="ms-2 text-muted">v1.0</small>
        </Navbar.Brand>
        
        <Navbar.Toggle aria-controls="basic-navbar-nav" />
        
        <Navbar.Collapse id="basic-navbar-nav">
          <Nav className="me-auto">
            <Nav.Link href="#upload">
              <i className="fas fa-upload me-1"></i>
              Upload
            </Nav.Link>
            <Nav.Link href="#comparison">
              <i className="fas fa-balance-scale me-1"></i>
              Comparison
            </Nav.Link>
            <Nav.Link href="#feedback">
              <i className="fas fa-comments me-1"></i>
              Feedback
            </Nav.Link>
          </Nav>
          
          <Nav>
            {overallSimilarity !== undefined && (
              <Nav.Item className="d-flex align-items-center me-3">
                <span className="text-light me-2">Overall Similarity:</span>
                <Badge 
                  bg={overallSimilarity >= 0.8 ? 'success' : overallSimilarity >= 0.6 ? 'warning' : 'danger'}
                  pill
                >
                  {Math.round(overallSimilarity * 100)}%
                </Badge>
              </Nav.Item>
            )}
            <Nav.Link href="#help">
              <i className="fas fa-question-circle me-1"></i>
              Help
            </Nav.Link>
          </Nav>
        </Navbar.Collapse>
      </Container>
    </Navbar>
  );
};

export default Header; 