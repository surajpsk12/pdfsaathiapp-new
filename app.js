/**
 * PDF Saathi - Main Application Logic (Interactive UI Prototype)
 */

document.addEventListener('DOMContentLoaded', () => {
  // --- Initialize State ---
  let documents = [...SAMPLE_DOCUMENTS];
  let folders = [...FOLDERS];
  let currentDoc = documents[0]; // Default active document
  let currentPage = 1;
  let currentTheme = 'light';
  let isScrollMode = true; // Continuous scroll vs single page
  let viewLayoutMode = 'list'; // File manager view mode: list or grid
  let currentFolder = null;
  let textAnnotations = []; // { page, text, type, color }

  // --- DOM Elements ---
  const deviceShell = document.getElementById('device-shell');
  const toggleFrameBtn = document.getElementById('toggle-frame-btn');
  const quickSplashBtn = document.getElementById('quick-splash-btn');
  const statusTime = document.getElementById('status-time');

  // Screens
  const screens = {
    splash: document.getElementById('screen-splash'),
    home: document.getElementById('screen-home'),
    files: document.getElementById('screen-files'),
    viewer: document.getElementById('screen-viewer'),
    favorites: document.getElementById('screen-favorites'),
    settings: document.getElementById('screen-settings')
  };

  // Nav Items
  const navItems = document.querySelectorAll('.nav-item');
  const mainBottomNav = document.getElementById('main-bottom-nav');

  // Viewer Controls
  const viewerTitle = document.getElementById('viewer-doc-title');
  const viewerSub = document.getElementById('viewer-doc-sub');
  const viewerViewport = document.getElementById('pdf-scroll-viewport');
  const viewerSlider = document.getElementById('viewer-page-slider');
  const pageBadge = document.getElementById('page-badge');
  const viewerStarBtn = document.getElementById('viewer-star-btn');
  const viewerBackBtn = document.getElementById('viewer-back-btn');
  const annoToolbar = document.getElementById('annotation-toolbar');

  // Modals & Toast
  const toast = document.getElementById('toast');
  const clearModal = document.getElementById('clear-modal');
  const clearRecentsBtn = document.getElementById('clear-recents-btn');
  const modalCancelBtn = document.getElementById('modal-cancel-btn');
  const modalConfirmBtn = document.getElementById('modal-confirm-btn');

  // --- Clock Updater ---
  function updateClock() {
    const now = new Date();
    const hrs = String(now.getHours()).padStart(2, '0');
    const mins = String(now.getMinutes()).padStart(2, '0');
    statusTime.textContent = `${hrs}:${mins}`;
  }
  updateClock();
  setInterval(updateClock, 1000);

  // --- Initialize Lucide Icons ---
  if (window.lucide) {
    lucide.createIcons();
  }

  // --- Helper Toast Notification ---
  function showToast(message) {
    toast.textContent = message;
    toast.classList.add('show');
    setTimeout(() => {
      toast.classList.remove('show');
    }, 2400);
  }

  // --- Screen Navigation Router ---
  function navigateToScreen(screenId) {
    Object.keys(screens).forEach(id => {
      screens[id].classList.remove('active');
    });

    if (screens[screenId]) {
      screens[screenId].classList.add('active');
    }

    // Toggle bottom nav bar visibility
    if (screenId === 'viewer' || screenId === 'splash') {
      mainBottomNav.style.display = 'none';
    } else {
      mainBottomNav.style.display = 'flex';
      // Update active nav pill
      navItems.forEach(item => {
        if (item.dataset.target === `screen-${screenId}`) {
          item.classList.add('active');
        } else {
          item.classList.remove('active');
        }
      });
    }
  }

  // Splash Screen Timeout
  setTimeout(() => {
    navigateToScreen('home');
  }, 1800);

  quickSplashBtn.addEventListener('click', () => {
    navigateToScreen('splash');
    setTimeout(() => {
      navigateToScreen('home');
    }, 1500);
  });

  // Toggle Device Frame / Fullscreen
  toggleFrameBtn.addEventListener('click', () => {
    deviceShell.classList.toggle('fullscreen');
  });

  // Nav Bar Click Routing
  navItems.forEach(item => {
    item.addEventListener('click', () => {
      const targetScreen = item.dataset.target.replace('screen-', '');
      navigateToScreen(targetScreen);
    });
  });

  // --- Render Home Screen Sections ---
  function renderHomeScreen() {
    // 1. Recent Files (Horizontal Row)
    const recentsContainer = document.getElementById('recents-card-row');
    recentsContainer.innerHTML = '';

    const recentDocs = documents.slice(0, 5); // top recent
    recentDocs.forEach(doc => {
      const card = document.createElement('div');
      card.className = 'recent-card';
      card.innerHTML = `
        <div class="card-thumbnail" style="background: ${doc.coverGradient}">
          <i data-lucide="file-text" class="card-thumbnail-icon"></i>
          <span class="card-page-badge">Pg ${doc.currentPage}/${doc.pages}</span>
        </div>
        <div class="card-title">${doc.name}</div>
        <div class="card-meta">${doc.size} • ${doc.lastOpened}</div>
      `;
      card.addEventListener('click', () => openPdfViewer(doc));
      recentsContainer.appendChild(card);
    });

    // 2. Favorites Row
    const favsContainer = document.getElementById('home-favs-row');
    favsContainer.innerHTML = '';

    const favDocs = documents.filter(d => d.isFavorite);
    if (favDocs.length === 0) {
      favsContainer.innerHTML = `<div style="font-size: 0.82rem; color: var(--md-on-surface-muted); padding: 10px 0;">No favorite documents yet. Tap star to save.</div>`;
    } else {
      favDocs.forEach(doc => {
        const card = document.createElement('div');
        card.className = 'recent-card';
        card.innerHTML = `
          <div class="card-thumbnail" style="background: ${doc.coverGradient}">
            <i data-lucide="star" class="card-thumbnail-icon" style="color: #F59E0B; fill: #F59E0B;"></i>
            <span class="card-page-badge">${doc.pages} Pages</span>
          </div>
          <div class="card-title">${doc.name}</div>
          <div class="card-meta">${doc.size}</div>
        `;
        card.addEventListener('click', () => openPdfViewer(doc));
        favsContainer.appendChild(card);
      });
    }

    // 3. All Documents List
    const allDocsContainer = document.getElementById('all-docs-list');
    allDocsContainer.innerHTML = '';

    documents.forEach(doc => {
      const item = document.createElement('div');
      item.className = 'doc-item-row';
      item.innerHTML = `
        <div class="doc-icon-box" style="background: ${doc.coverGradient}">
          <i data-lucide="file-text"></i>
        </div>
        <div class="doc-info">
          <div class="doc-name">${doc.name}</div>
          <div class="doc-details">${doc.size} • ${doc.pages} Pages • ${doc.lastOpened}</div>
        </div>
        <div class="star-btn ${doc.isFavorite ? 'starred' : ''}" data-id="${doc.id}">
          <i data-lucide="star" style="${doc.isFavorite ? 'fill: currentColor;' : ''}"></i>
        </div>
      `;

      item.querySelector('.doc-info').addEventListener('click', () => openPdfViewer(doc));
      item.querySelector('.star-btn').addEventListener('click', (e) => {
        e.stopPropagation();
        toggleFavorite(doc.id);
      });

      allDocsContainer.appendChild(item);
    });

    if (window.lucide) lucide.createIcons();
  }

  // --- Toggle Favorite Helper ---
  function toggleFavorite(docId) {
    const doc = documents.find(d => d.id === docId);
    if (doc) {
      doc.isFavorite = !doc.isFavorite;
      showToast(doc.isFavorite ? 'Added to Favorites' : 'Removed from Favorites');
      renderHomeScreen();
      renderFavoritesScreen();
      if (currentDoc.id === docId) {
        updateViewerStarIcon();
      }
    }
  }

  // --- Render Favorites Screen ---
  function renderFavoritesScreen() {
    const favsList = document.getElementById('favs-full-list');
    favsList.innerHTML = '';

    const favDocs = documents.filter(d => d.isFavorite);
    if (favDocs.length === 0) {
      favsList.innerHTML = `
        <div class="empty-state-box">
          <div class="empty-state-icon"><i data-lucide="star-off" style="width: 42px; height: 42px;"></i></div>
          <div class="empty-title">No Favorite PDFs</div>
          <div class="empty-subtitle">Tap the star icon on any document card to quickly access it here offline.</div>
        </div>
      `;
    } else {
      favDocs.forEach(doc => {
        const item = document.createElement('div');
        item.className = 'doc-item-row';
        item.innerHTML = `
          <div class="doc-icon-box" style="background: ${doc.coverGradient}">
            <i data-lucide="file-text"></i>
          </div>
          <div class="doc-info">
            <div class="doc-name">${doc.name}</div>
            <div class="doc-details">${doc.size} • ${doc.pages} Pages</div>
          </div>
          <div class="star-btn starred" data-id="${doc.id}">
            <i data-lucide="star" style="fill: currentColor;"></i>
          </div>
        `;
        item.querySelector('.doc-info').addEventListener('click', () => openPdfViewer(doc));
        item.querySelector('.star-btn').addEventListener('click', (e) => {
          e.stopPropagation();
          toggleFavorite(doc.id);
        });
        favsList.appendChild(item);
      });
    }
    if (window.lucide) lucide.createIcons();
  }

  // --- Render File Manager Screen ---
  function renderFileManager() {
    const folderContainer = document.getElementById('folder-container');
    folderContainer.innerHTML = '';

    folders.forEach(f => {
      const card = document.createElement('div');
      card.className = 'folder-card';
      card.innerHTML = `
        <i data-lucide="folder" class="folder-icon"></i>
        <div>
          <div class="folder-name">${f.name}</div>
          <div class="folder-count">${f.count} documents</div>
        </div>
      `;
      folderContainer.appendChild(card);
    });

    const fileContent = document.getElementById('file-explorer-content');
    fileContent.innerHTML = '';

    if (viewLayoutMode === 'list') {
      fileContent.className = 'doc-list-vertical';
      documents.forEach(doc => {
        const item = document.createElement('div');
        item.className = 'doc-item-row';
        item.innerHTML = `
          <div class="doc-icon-box" style="background: ${doc.coverGradient}">
            <i data-lucide="file-text"></i>
          </div>
          <div class="doc-info">
            <div class="doc-name">${doc.name}</div>
            <div class="doc-details">${doc.size} • ${doc.pages} Pgs</div>
          </div>
        `;
        item.addEventListener('click', () => openPdfViewer(doc));
        fileContent.appendChild(item);
      });
    } else {
      fileContent.className = 'doc-grid-layout';
      documents.forEach(doc => {
        const item = document.createElement('div');
        item.className = 'doc-grid-item';
        item.innerHTML = `
          <div class="doc-grid-thumb" style="background: ${doc.coverGradient}">
            <i data-lucide="file-text" style="width: 32px; height: 32px;"></i>
          </div>
          <div class="doc-name" style="font-size: 0.82rem;">${doc.name}</div>
          <div class="doc-details">${doc.size}</div>
        `;
        item.addEventListener('click', () => openPdfViewer(doc));
        fileContent.appendChild(item);
      });
    }

    if (window.lucide) lucide.createIcons();
  }

  // View Mode Toggles (List vs Grid)
  document.getElementById('view-list-btn').addEventListener('click', () => {
    viewLayoutMode = 'list';
    document.getElementById('view-list-btn').classList.add('active');
    document.getElementById('view-grid-btn').classList.remove('active');
    renderFileManager();
  });

  document.getElementById('view-grid-btn').addEventListener('click', () => {
    viewLayoutMode = 'grid';
    document.getElementById('view-grid-btn').classList.add('active');
    document.getElementById('view-list-btn').classList.remove('active');
    renderFileManager();
  });

  // --- Screen 4: PDF Viewer Engine ---
  function openPdfViewer(doc) {
    currentDoc = doc;
    currentPage = doc.currentPage || 1;
    viewerTitle.textContent = doc.name;
    viewerSub.textContent = `Page ${currentPage} of ${doc.pages}`;

    updateViewerStarIcon();
    renderPdfPages();
    navigateToScreen('viewer');

    showToast(`Opened ${doc.name}`);
  }

  function updateViewerStarIcon() {
    if (currentDoc.isFavorite) {
      viewerStarBtn.style.color = '#F59E0B';
      viewerStarBtn.style.fill = '#F59E0B';
    } else {
      viewerStarBtn.style.color = 'var(--md-on-surface)';
      viewerStarBtn.style.fill = 'none';
    }
  }

  viewerStarBtn.addEventListener('click', () => {
    toggleFavorite(currentDoc.id);
  });

  viewerBackBtn.addEventListener('click', () => {
    // Save current position
    currentDoc.currentPage = currentPage;
    navigateToScreen('home');
  });

  function renderPdfPages() {
    viewerViewport.innerHTML = '';
    const totalPages = currentDoc.pages;

    viewerSlider.max = totalPages;
    viewerSlider.value = currentPage;
    pageBadge.textContent = `${currentPage} / ${totalPages}`;

    const contentList = currentDoc.content || [];

    for (let p = 1; p <= totalPages; p++) {
      const pagePaper = document.createElement('div');
      pagePaper.className = 'pdf-page-paper';
      pagePaper.dataset.page = p;

      const matchedContent = contentList.find(c => c.page === p) || {
        title: `${currentDoc.name.replace('.pdf', '')} - Section ${p}`,
        subtitle: `Page ${p} Reading Content`,
        body: [
          `This is page ${p} of ${currentDoc.name}. High-performance offline PDF reading mode active.`,
          `Study notes and reference materials are rendered smoothly with Material 3 typography and crisp contrast.`,
          `Select text anywhere on this document to trigger the floating annotation toolbar (Highlight, Underline, Strikethrough, Color Picker).`
        ]
      };

      let bodyHtml = matchedContent.body.map(para => `<p class="pdf-page-paragraph">${para}</p>`).join('');

      pagePaper.innerHTML = `
        <div class="pdf-page-header">${matchedContent.title}</div>
        <div class="pdf-page-sub">${matchedContent.subtitle}</div>
        <div style="margin-top: 14px;">${bodyHtml}</div>
        <span class="pdf-page-number">Page ${p}</span>
      `;

      pagePaper.addEventListener('mouseup', handleTextSelection);

      viewerViewport.appendChild(pagePaper);
    }
  }

  // Text Selection & Annotation Toolbar Trigger
  function handleTextSelection(e) {
    const selection = window.getSelection();
    if (selection && selection.toString().trim().length > 0) {
      annoToolbar.classList.remove('hidden');
    } else {
      annoToolbar.classList.add('hidden');
    }
  }

  // Annotation Colors & Tools
  const colorDots = document.querySelectorAll('.color-dot');
  colorDots.forEach(dot => {
    dot.addEventListener('click', () => {
      colorDots.forEach(d => d.classList.remove('selected'));
      dot.classList.add('selected');
    });
  });

  document.getElementById('tool-highlight').addEventListener('click', () => {
    applyTextAnnotation('pdf-highlight');
  });
  document.getElementById('tool-underline').addEventListener('click', () => {
    applyTextAnnotation('pdf-underline');
  });
  document.getElementById('tool-strikethrough').addEventListener('click', () => {
    applyTextAnnotation('pdf-strikethrough');
  });

  function applyTextAnnotation(className) {
    const selection = window.getSelection();
    if (!selection || selection.rangeCount === 0) return;

    const range = selection.getRangeAt(0);
    const span = document.createElement('span');
    span.className = className;

    const selectedDot = document.querySelector('.color-dot.selected');
    if (selectedDot && className === 'pdf-highlight') {
      span.style.backgroundColor = selectedDot.dataset.color;
    }

    range.surroundContents(span);
    selection.removeAllRanges();
    annoToolbar.classList.add('hidden');
    showToast('Annotation saved!');
  }

  // Slider & Page Navigation
  viewerSlider.addEventListener('input', (e) => {
    currentPage = parseInt(e.target.value);
    pageBadge.textContent = `${currentPage} / ${currentDoc.pages}`;
    viewerSub.textContent = `Page ${currentPage} of ${currentDoc.pages}`;

    // Scroll to page
    const targetPaper = viewerViewport.querySelector(`[data-page="${currentPage}"]`);
    if (targetPaper) {
      targetPaper.scrollIntoView({ behavior: 'smooth', block: 'start' });
    }
  });

  document.getElementById('prev-page-btn').addEventListener('click', () => {
    if (currentPage > 1) {
      currentPage--;
      viewerSlider.value = currentPage;
      viewerSlider.dispatchEvent(new Event('input'));
    }
  });

  document.getElementById('next-page-btn').addEventListener('click', () => {
    if (currentPage < currentDoc.pages) {
      currentPage++;
      viewerSlider.value = currentPage;
      viewerSlider.dispatchEvent(new Event('input'));
    }
  });

  // Theme Mode Switcher in Viewer (Light / Dark / Sepia)
  const floatThemeBtn = document.getElementById('float-theme-btn');
  floatThemeBtn.addEventListener('click', () => {
    const htmlEl = document.documentElement;
    if (currentTheme === 'light') {
      currentTheme = 'dark';
      htmlEl.setAttribute('data-theme', 'dark');
      showToast('Dark Reading Theme');
    } else if (currentTheme === 'dark') {
      currentTheme = 'sepia';
      htmlEl.setAttribute('data-theme', 'sepia');
      showToast('Warm Sepia Reading Theme');
    } else {
      currentTheme = 'light';
      htmlEl.setAttribute('data-theme', 'light');
      showToast('Light Reading Theme');
    }
  });

  // Bookmark Button
  document.getElementById('float-bookmark-btn').addEventListener('click', () => {
    showToast(`Bookmarked Page ${currentPage}`);
  });

  // --- FAB PDF File Import Simulation ---
  const fabImportBtn = document.getElementById('fab-import-btn');
  const pdfFileInput = document.getElementById('pdf-file-input');

  fabImportBtn.addEventListener('click', () => {
    pdfFileInput.click();
  });

  pdfFileInput.addEventListener('change', (e) => {
    const file = e.target.files[0];
    if (file) {
      const newDoc = {
        id: `imported-${Date.now()}`,
        name: file.name,
        size: `${(file.size / (1024 * 1024)).toFixed(1)} MB`,
        pages: 15,
        currentPage: 1,
        lastOpened: 'Just now',
        category: 'Imports',
        isFavorite: false,
        coverGradient: 'linear-gradient(135deg, #059669 0%, #3B82F6 100%)',
        content: [
          {
            page: 1,
            title: file.name.replace('.pdf', ''),
            subtitle: 'Imported PDF Document Overview',
            body: [
              `Successfully imported local document: ${file.name}`,
              `File size: ${(file.size / 1024).toFixed(0)} KB. Memory-safe canvas parsing active.`
            ]
          }
        ]
      };

      documents.unshift(newDoc);
      renderHomeScreen();
      renderFileManager();
      showToast(`Imported ${file.name}`);
      openPdfViewer(newDoc);
    }
  });

  // --- Settings & Modal Handlers ---
  const themeBtns = document.querySelectorAll('#theme-segmented .segmented-btn');
  themeBtns.forEach(btn => {
    btn.addEventListener('click', () => {
      themeBtns.forEach(b => b.classList.remove('active'));
      btn.classList.add('active');

      const val = btn.dataset.val;
      currentTheme = val;
      document.documentElement.setAttribute('data-theme', val);
      showToast(`Applied ${val.toUpperCase()} theme`);
    });
  });

  clearRecentsBtn.addEventListener('click', () => {
    clearModal.classList.add('active');
  });

  modalCancelBtn.addEventListener('click', () => {
    clearModal.classList.remove('active');
  });

  modalConfirmBtn.addEventListener('click', () => {
    documents = documents.filter(d => d.isFavorite);
    renderHomeScreen();
    renderFileManager();
    clearModal.classList.remove('active');
    showToast('Recent files cleared');
  });

  // Initial renders
  renderHomeScreen();
  renderFavoritesScreen();
  renderFileManager();
});
