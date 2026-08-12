/**
 * Sample Documents & PDF Generator Data for PDF Saathi App
 */

const SAMPLE_DOCUMENTS = [
  {
    id: 'doc-1',
    name: 'Data Structures & Algorithms.pdf',
    size: '4.2 MB',
    pages: 142,
    currentPage: 12,
    lastOpened: '10 mins ago',
    category: 'Textbooks',
    folder: 'Computer Science',
    isFavorite: true,
    coverGradient: 'linear-gradient(135deg, #4F46E5 0%, #3B82F6 100%)',
    icon: 'code-square',
    content: [
      {
        page: 1,
        title: 'Data Structures & Algorithms Masterclass',
        subtitle: 'Chapter 4: Binary Search Trees & AVL Balancing',
        body: [
          'Binary search trees (BST) provide efficient O(log n) time complexity for search, insertion, and deletion operations when balanced.',
          'An AVL tree is a self-balancing binary search tree where the heights of the two child subtrees of any node differ by at most one.',
          'Key Operations & Complexity:',
          '• Search: O(log n) average, O(n) worst case (unbalanced)',
          '• Insertion: O(log n) with rotational balance checks',
          '• Deletion: O(log n) utilizing in-order predecessor or successor swap',
          'Rotations in AVL Trees: Single Left (LL), Single Right (RR), Double Left-Right (LR), and Double Right-Left (RL).'
        ]
      },
      {
        page: 2,
        title: 'Tree Traversal Algorithms',
        subtitle: 'In-Order, Pre-Order, and Post-Order Techniques',
        body: [
          'Tree traversal is the process of visiting each node in a tree data structure exactly once.',
          '1. In-Order (Left, Root, Right): Produces sorted output for Binary Search Trees.',
          '2. Pre-Order (Root, Left, Right): Useful for copying tree structures or prefix notation.',
          '3. Post-Order (Left, Right, Root): Useful for deleting trees or postfix evaluation.',
          'Level-Order Traversal utilizes a Queue (FIFO) data structure to visit nodes level by level from top to bottom.'
        ]
      },
      {
        page: 3,
        title: 'Graph Theory Foundations',
        subtitle: 'Breadth-First Search (BFS) vs Depth-First Search (DFS)',
        body: [
          'Graphs consist of vertices (V) and edges (E). They can be directed or undirected, weighted or unweighted.',
          'BFS Uses Queue | Time Complexity: O(V + E) | Space: O(V) | Finds shortest path in unweighted graphs.',
          'DFS Uses Stack / Recursion | Time Complexity: O(V + E) | Space: O(V) | Useful for topological sorting and cycle detection.'
        ]
      }
    ]
  },
  {
    id: 'doc-2',
    name: 'Quantum Physics Lecture Notes.pdf',
    size: '8.7 MB',
    pages: 64,
    currentPage: 5,
    lastOpened: '2 hours ago',
    category: 'Physics',
    folder: 'Semester 4',
    isFavorite: true,
    coverGradient: 'linear-gradient(135deg, #7C3AED 0%, #EC4899 100%)',
    icon: 'atom',
    content: [
      {
        page: 1,
        title: 'Quantum Mechanics & Wave Functions',
        subtitle: 'Schrödinger Equation & Wave-Particle Duality',
        body: [
          'The Schrödinger equation is a linear partial differential equation that governs the wave function of a quantum-mechanical system.',
          'iℏ ∂/∂t Ψ(r,t) = Ĥ Ψ(r,t)',
          'The probability density of finding a particle at position r is given by |Ψ(r,t)|².',
          'Heisenberg Uncertainty Principle: Δx Δp ≥ ℏ / 2',
          'It is impossible to simultaneously measure position and momentum with arbitrary precision.'
        ]
      },
      {
        page: 2,
        title: 'Quantum Entanglement & Superposition',
        subtitle: 'EPR Paradox & Bell Inequalities',
        body: [
          'Superposition states that any quantum state can be represented as a linear sum of two or more pure states.',
          'Entanglement occurs when pairs or groups of particles interact such that the quantum state of each particle cannot be described independently.',
          'Bell test experiments demonstrate that quantum physics violates local realism.'
        ]
      }
    ]
  },
  {
    id: 'doc-3',
    name: 'Mobile UX/UI Design Guidelines.pdf',
    size: '14.1 MB',
    pages: 98,
    currentPage: 1,
    lastOpened: 'Yesterday',
    category: 'Design',
    folder: 'Reference',
    isFavorite: false,
    coverGradient: 'linear-gradient(135deg, #2563EB 0%, #06B6D4 100%)',
    icon: 'palette',
    content: [
      {
        page: 1,
        title: 'Material 3 Design Principles for Mobile',
        subtitle: 'Adapting Color, Elevation & Typography',
        body: [
          'Material 3 (M3) introduces dynamic color schemes that adapt to user preferences and context.',
          'Key Highlights:',
          '• Tonal Palettes: 13 system tones for harmonious light/dark themes.',
          '• Floating Action Buttons (FAB): Soft rounded squircle shapes with clear micro-interactions.',
          '• Spacing & Margins: Standard 16dp horizontal padding for compact mobile layouts.',
          '• Typography: Expressive type scales ensuring high legibility during extended reading sessions.'
        ]
      }
    ]
  },
  {
    id: 'doc-4',
    name: 'Macroeconomics Ch 4 - Inflation & GDP.pdf',
    size: '2.5 MB',
    pages: 35,
    currentPage: 18,
    lastOpened: '3 days ago',
    category: 'Economics',
    folder: 'Semester 4',
    isFavorite: false,
    coverGradient: 'linear-gradient(135deg, #059669 0%, #10B981 100%)',
    icon: 'bar-chart',
    content: [
      {
        page: 1,
        title: 'Gross Domestic Product & Inflation Analysis',
        subtitle: 'Measuring Aggregate Output & Consumer Price Index',
        body: [
          'GDP is the total monetary or market value of all the finished goods and services produced within a country in a specific time period.',
          'Formula: GDP = C + I + G + (X - M)',
          '• C: Consumption by households',
          '• I: Investment by businesses',
          '• G: Government expenditures',
          '• X - M: Net Exports (Exports minus Imports)'
        ]
      }
    ]
  },
  {
    id: 'doc-5',
    name: 'Organic Chemistry Quick Formula Sheet.pdf',
    size: '1.8 MB',
    pages: 12,
    currentPage: 2,
    lastOpened: '5 days ago',
    category: 'Chemistry',
    folder: 'Reference',
    isFavorite: true,
    coverGradient: 'linear-gradient(135deg, #D97706 0%, #F59E0B 100%)',
    icon: 'flask',
    content: [
      {
        page: 1,
        title: 'Organic Reaction Mechanisms Overview',
        subtitle: 'Nucleophilic Substitution (SN1 vs SN2) & Elimination',
        body: [
          'SN1 Reaction: Two-step mechanism, proceeds via carbocation intermediate. Favored by tertiary alkyl halides and polar protic solvents.',
          'SN2 Reaction: One-step concerted mechanism with inversion of stereochemistry. Favored by primary alkyl halides and strong nucleophiles.'
        ]
      }
    ]
  }
];

const FOLDERS = [
  { id: 'f-1', name: 'Computer Science', count: 12, path: 'Home / Computer Science' },
  { id: 'f-2', name: 'Semester 4', count: 8, path: 'Home / Semester 4' },
  { id: 'f-3', name: 'Reference', count: 5, path: 'Home / Reference' },
  { id: 'f-4', name: 'Downloads', count: 18, path: 'Home / Downloads' }
];
