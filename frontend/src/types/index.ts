export interface CodeElement {
  id: string;
  name: string;
  type: 'class' | 'field' | 'method' | 'constructor';
  signature: string;
  sourceCode: string;
  lineNumber: number;
  matched: boolean;
  matchType: 'exact' | 'similar' | 'missing' | 'extra';
  similarity?: number;
}

export interface CodeStructure {
  id: string;
  fileName: string;
  packageName: string;
  classes: CodeElement[];
  fields: CodeElement[];
  methods: CodeElement[];
  constructors: CodeElement[];
}

export interface ComparisonResult {
  id: string;
  studentCode: CodeStructure[];
  referenceCode: CodeStructure[];
  matchedElements: ComparisonMatch[];
  unmatchedStudentElements: CodeElement[];
  unmatchedReferenceElements: CodeElement[];
  overallSimilarity: number;
}

export interface ComparisonMatch {
  studentElement: CodeElement;
  referenceElement: CodeElement;
  similarity: number;
  differences: string[];
}

export interface Feedback {
  id: string;
  comparisonId: string;
  elementId: string;
  score: number; // 0-100
  comments: string;
  designPatternFeedback: string;
  bestPracticesFeedback: string;
  createdAt: Date;
  updatedAt: Date;
}

export interface UploadedFile {
  id: string;
  name: string;
  size: number;
  type: 'student' | 'reference';
  status: 'uploaded' | 'processing' | 'completed' | 'error';
  uploadDate: Date;
  errorMessage?: string;
}

export interface GradingSession {
  id: string;
  studentFile: UploadedFile;
  referenceFile: UploadedFile;
  comparison?: ComparisonResult;
  feedbacks: Feedback[];
  overallScore?: number;
  finalComments?: string;
  status: 'created' | 'analyzing' | 'ready' | 'completed';
  createdAt: Date;
  updatedAt: Date;
}

export interface APIResponse<T> {
  success: boolean;
  data: T;
  message?: string;
  error?: string;
}

export interface UploadProgress {
  loaded: number;
  total: number;
  percentage: number;
} 