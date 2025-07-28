# DiffGrader Frontend

This is the React + TypeScript frontend for the DiffGrader intelligent code comparison and feedback grading system.

## Features

- 📁 **File Upload**: Drag-and-drop interface for uploading ZIP files
- 🔍 **Code Comparison**: Side-by-side code comparison with Monaco Editor
- 📝 **Feedback System**: Comprehensive grading and feedback interface
- 🎯 **Real-time Analysis**: Live updates during code analysis
- 📱 **Responsive Design**: Works on desktop and mobile devices

## Technology Stack

- **React 18** with TypeScript
- **React Bootstrap** for UI components
- **Monaco Editor** for code visualization
- **Axios** for API communication
- **CSS3** with custom styling

## Getting Started

### Prerequisites

- Node.js 16+
- npm or yarn

### Installation

1. **Navigate to frontend directory**
   ```bash
   cd frontend
   ```

2. **Install dependencies**
   ```bash
   npm install
   ```

3. **Configure environment**
   ```bash
   cp .env.example .env
   # Edit .env file with your backend API URL
   ```

4. **Start development server**
   ```bash
   npm start
   ```

The application will be available at `http://localhost:3000`.

### Building for Production

```bash
npm run build
```

This builds the app for production to the `build` folder.

## Project Structure

```
frontend/
├── public/                 # Static files
├── src/
│   ├── components/        # React components
│   │   ├── FileUpload.tsx
│   │   ├── CodeComparison.tsx
│   │   ├── FeedbackPanel.tsx
│   │   └── Header.tsx
│   ├── services/          # API services
│   │   └── api.ts
│   ├── types/             # TypeScript type definitions
│   │   └── index.ts
│   ├── utils/             # Utility functions
│   │   └── helpers.ts
│   ├── App.tsx            # Main application component
│   ├── App.css            # Application styles
│   └── index.tsx          # Application entry point
├── package.json
├── tsconfig.json
└── README.md
```

## Key Components

### FileUpload
- Handles ZIP file upload with drag-and-drop
- File validation and progress tracking
- Supports both student and reference files

### CodeComparison
- Side-by-side code comparison using Monaco Editor
- Element navigation (classes, methods, fields, constructors)
- Similarity highlighting and difference detection

### FeedbackPanel
- Interactive grading interface
- Score slider and comment fields
- Auto-suggestion based on code analysis

## API Integration

The frontend communicates with the backend through RESTful APIs:

- `POST /api/files/upload` - File upload
- `POST /api/grading-sessions` - Create grading session
- `GET /api/grading-sessions/:id/comparison` - Get comparison results
- `POST /api/grading-sessions/:id/feedback` - Submit feedback

## Environment Variables

Create a `.env` file in the frontend directory:

```env
REACT_APP_API_BASE_URL=http://localhost:8080/api
REACT_APP_ENV=development
REACT_APP_VERSION=1.0.0
```

## Available Scripts

- `npm start` - Start development server
- `npm build` - Build for production
- `npm test` - Run tests
- `npm eject` - Eject from Create React App (not recommended)

## Browser Support

- Chrome (latest)
- Firefox (latest)
- Safari (latest)
- Edge (latest)

## Performance Optimization

- Code splitting with React.lazy()
- Monaco Editor lazy loading
- Optimized bundle size
- Image optimization
- CSS minification

## Styling

The application uses:
- Bootstrap 5 for base components
- Custom CSS for specific styling
- Font Awesome icons
- Responsive design principles

## Development Guidelines

1. **Component Structure**: Use functional components with hooks
2. **TypeScript**: Maintain strict type safety
3. **Error Handling**: Implement comprehensive error boundaries
4. **Performance**: Use React.memo() for expensive components
5. **Accessibility**: Follow WCAG guidelines

## Troubleshooting

### Common Issues

1. **Module not found errors**: Run `npm install`
2. **API connection issues**: Check backend server and CORS settings
3. **TypeScript errors**: Ensure all dependencies are installed
4. **Build fails**: Clear node_modules and reinstall

### Debug Mode

Set `REACT_APP_ENV=development` to enable debug logging.

## Contributing

1. Follow the existing code style
2. Add TypeScript types for new features
3. Include unit tests for new components
4. Update documentation as needed

## License

This project is licensed under the MIT License. 