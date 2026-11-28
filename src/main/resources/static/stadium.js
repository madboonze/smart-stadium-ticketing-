const $ = (id) => document.getElementById(id);
const state = { stadium: null, match: null, seats: [], selected: new Set(), three: null, scene: null, camera: null, renderer: null, seatMeshes: new Map(), raycaster: null, pointer: null, target: { x: 0, y: 1.1, z: 0 }, theta: 0.18, phi: 1.07, radius: 29, toastTimer: null };

const money = (value) => new Intl.NumberFormat('en-AE', { style: 'currency', currency: 'AED', maximumFractionDigits: 0 }).format(Number(value || 0));
const categoryName = (value) => ({ REGULAR: 'Regular', PREMIUM: 'Premium', VIP: 'VIP' }[value] || String(value || 'Seat').replaceAll('_', ' '));
const statusName = (value) => ({ AVAILABLE: 'Available', HELD: 'On hold', BOOKED: 'Booked' }[value] || 'Unavailable');
const isAvailable = (seat) => seat.status === 'AVAILABLE';

function showMessage(message) { $('scene-message').textContent = message || ''; }

function showToast(message) {
  const toast = $('toast');
  toast.textContent = message;
  toast.classList.add('show');
  clearTimeout(state.toastTimer);
  state.toastTimer = setTimeout(() => toast.classList.remove('show'), 3300);
}

function setEventDetails(data) {
  state.stadium = data;
  state.match = data.match || {};
  $('stadium-name').textContent = data.name || 'Stadium';
  $('stadium-city').textContent = String(data.city || 'STADIUM').toLocaleUpperCase();
  $('home-team').textContent = state.match.homeTeam || 'Home team';
  $('away-team').textContent = state.match.awayTeam || 'Away team';
  $('match-status').textContent = state.match.status ? String(state.match.status).replaceAll('_', ' ') : 'MATCH DAY';
  $('match-subtitle').textContent = 'A great night of football starts with the right seat.';
  if (state.match.kickoff) {
    const date = new Date(state.match.kickoff);
    $('kickoff').textContent = Number.isNaN(date.getTime()) ? state.match.kickoff : new Intl.DateTimeFormat(undefined, { weekday: 'short', day: 'numeric', month: 'short', hour: 'numeric', minute: '2-digit' }).format(date);
  } else {
    $('kickoff').textContent = 'Match details coming soon';
  }
  state.seats = (data.stands || []).flatMap((stand, standIndex) => (stand.seats || []).map((seat, index) => ({ ...seat, standId: stand.id, standName: stand.name, standIndex, standSeatIndex: index })));
  $('available-count').textContent = state.seats.filter(isAvailable).length + ' of ' + state.seats.length + ' seats available';
}

function renderSeatList() {
  const list = $('seat-list');
  list.replaceChildren();
  if (!state.seats.length) {
    const empty = document.createElement('div');
    empty.className = 'empty-state';
    const title = document.createElement('strong');
    title.textContent = 'No seat inventory yet';
    const note = document.createElement('span');
    note.textContent = 'Seat inventory will appear here when it is added to the stadium.';
    empty.append(title, note);
    list.append(empty);
    updateSummary();
    return;
  }
  state.seats.forEach((seat) => {
    const selected = state.selected.has(seat.id);
    const button = document.createElement('button');
    button.type = 'button';
    button.className = 'seat-row' + (selected ? ' is-selected' : '');
    button.disabled = !isAvailable(seat) && !selected;
    button.setAttribute('aria-pressed', selected ? 'true' : 'false');
    button.setAttribute('aria-label', seat.label + ', ' + seat.standName + ', ' + categoryName(seat.category) + ', ' + statusName(seat.status) + ', ' + money(seat.price));
    const dot = document.createElement('span');
    dot.className = 'seat-status' + (seat.status === 'HELD' ? ' held' : seat.status === 'BOOKED' ? ' booked' : '');
    const copy = document.createElement('span');
    copy.className = 'seat-copy';
    const label = document.createElement('strong');
    label.textContent = seat.label || seat.id;
    const stand = document.createElement('small');
    stand.textContent = seat.standName + ' · ' + statusName(seat.status);
    copy.append(label, stand);
    const category = document.createElement('span');
    category.className = 'seat-category';
    category.textContent = categoryName(seat.category);
    const price = document.createElement('span');
    price.className = 'seat-price';
    price.textContent = money(seat.price);
    button.append(dot, copy, category, price);
    button.addEventListener('click', () => toggleSeat(seat.id));
    list.append(button);
  });
  updateSummary();
}

function updateSummary() {
  const selectedSeats = state.seats.filter((seat) => state.selected.has(seat.id));
  $('selection-count').textContent = String(selectedSeats.length);
  $('total-price').textContent = money(selectedSeats.reduce((sum, seat) => sum + Number(seat.price || 0), 0));
  if (state.three) state.seats.forEach(updateSeatColor);
}

function toggleSeat(id) {
  const seat = state.seats.find((item) => item.id === id);
  if (!seat || (!isAvailable(seat) && !state.selected.has(id))) return;
  if (state.selected.has(id)) state.selected.delete(id); else state.selected.add(id);
  renderSeatList();
}

function seatColor(seat) {
  if (state.selected.has(seat.id)) return 0xf6bd65;
  if (seat.status === 'BOOKED') return 0xff7182;
  if (seat.status === 'HELD') return 0xaf98ff;
  return 0x68e1ae;
}

function updateSeatColor(seat) {
  const mesh = state.seatMeshes.get(seat.id);
  if (!mesh) return;
  const color = seatColor(seat);
  mesh.traverse((object) => {
    if (!object.isMesh || !object.material) return;
    object.material.color.setHex(color);
    object.material.emissive.setHex(state.selected.has(seat.id) ? 0x4b2b0b : isAvailable(seat) ? 0x0c3828 : 0x160c18);
  });
  mesh.scale.setScalar(state.selected.has(seat.id) ? 1.22 : 1);
}

function addRingTier(THREE, innerX, innerZ, outerX, outerZ, innerY, outerY, material) {
  const positions = [];
  const indices = [];
  const segments = 112;
  for (let i = 0; i <= segments; i++) {
    const angle = (i / segments) * Math.PI * 2;
    const c = Math.cos(angle), s = Math.sin(angle);
    positions.push(c * innerX, innerY, s * innerZ, c * outerX, outerY, s * outerZ);
  }
  for (let i = 0; i < segments; i++) {
    const a = i * 2, b = a + 1, c = a + 3, d = a + 2;
    indices.push(a, b, c, a, c, d);
  }
  const geometry = new THREE.BufferGeometry();
  geometry.setAttribute('position', new THREE.Float32BufferAttribute(positions, 3));
  geometry.setIndex(indices);
  geometry.computeVertexNormals();
  const mesh = new THREE.Mesh(geometry, material);
  mesh.receiveShadow = true;
  state.scene.add(mesh);
  return mesh;
}

function addEllipseLine(THREE, rx, rz, y, color, opacity) {
  const points = [];
  for (let i = 0; i <= 128; i++) {
    const angle = (i / 128) * Math.PI * 2;
    points.push(new THREE.Vector3(Math.cos(angle) * rx, y, Math.sin(angle) * rz));
  }
  const geometry = new THREE.BufferGeometry().setFromPoints(points);
  const line = new THREE.Line(geometry, new THREE.LineBasicMaterial({ color, transparent: opacity < 1, opacity }));
  state.scene.add(line);
}

function addPitchLine(THREE, points, material) {
  const geometry = new THREE.BufferGeometry().setFromPoints(points.map((point) => new THREE.Vector3(point[0], point[1] ?? 0.09, point[2])));
  state.scene.add(new THREE.Line(geometry, material));
}

function addPitch(THREE) {
  const grass = new THREE.Mesh(new THREE.PlaneGeometry(19, 11.4), new THREE.MeshStandardMaterial({ color: 0x286947, roughness: 0.92 }));
  grass.rotation.x = -Math.PI / 2;
  grass.position.y = 0.08;
  state.scene.add(grass);
  for (let i = 0; i < 8; i++) {
    const stripe = new THREE.Mesh(new THREE.PlaneGeometry(19 / 8, 11.4), new THREE.MeshBasicMaterial({ color: i % 2 ? 0x2c704c : 0x337c52, transparent: true, opacity: 0.27 }));
    stripe.rotation.x = -Math.PI / 2;
    stripe.position.set(-19 / 2 + (i + 0.5) * 19 / 8, 0.083, 0);
    state.scene.add(stripe);
  }
  const white = new THREE.LineBasicMaterial({ color: 0xc8e7d0, transparent: true, opacity: 0.76 });
  addPitchLine(THREE, [[-9.5, 0.095, -5.7], [9.5, 0.095, -5.7], [9.5, 0.095, 5.7], [-9.5, 0.095, 5.7], [-9.5, 0.095, -5.7]], white);
  addPitchLine(THREE, [[0, 0.096, -5.7], [0, 0.096, 5.7]], white);
  addPitchLine(THREE, [[-9.5, 0.097, -2.2], [-6.5, 0.097, -2.2], [-6.5, 0.097, 2.2], [-9.5, 0.097, 2.2]], white);
  addPitchLine(THREE, [[9.5, 0.097, -2.2], [6.5, 0.097, -2.2], [6.5, 0.097, 2.2], [9.5, 0.097, 2.2]], white);
  const circle = [];
  for (let i = 0; i <= 56; i++) { const a = i / 56 * Math.PI * 2; circle.push([Math.cos(a) * 1.65, 0.097, Math.sin(a) * 1.65]); }
  addPitchLine(THREE, circle, white);
  [[-9.5, -1.15], [9.5, 1.15]].forEach(([x, direction]) => {
    const goal = new THREE.Group();
    const frame = new THREE.LineBasicMaterial({ color: 0xd9f0df, transparent: true, opacity: 0.78 });
    const x2 = x + direction;
    [[x, -1.15, -0.9, x2, -1.15, -0.9], [x, 1.15, -0.9, x2, 1.15, -0.9], [x, -1.15, 0.9, x2, -1.15, 0.9], [x, 1.15, 0.9, x2, 1.15, 0.9], [x2, -1.15, -0.9, x2, 1.15, -0.9], [x2, -1.15, 0.9, x2, 1.15, 0.9], [x2, -1.15, -0.9, x2, -1.15, 0.9], [x2, 1.15, -0.9, x2, 1.15, 0.9]].forEach((coords) => {
      const line = new THREE.Line(new THREE.BufferGeometry().setFromPoints([new THREE.Vector3(coords[0], coords[1] + 0.12, coords[2]), new THREE.Vector3(coords[3], coords[4] + 0.12, coords[5])]), frame);
      goal.add(line);
    });
    state.scene.add(goal);
  });
}

function standCenter(stand, index) {
  const name = (stand.standName + ' ' + stand.standId).toLowerCase();
  if (name.includes('north') || name.includes('upper')) return -Math.PI / 2;
  if (name.includes('south') || name.includes('lower')) return Math.PI / 2;
  if (name.includes('east') || name.includes('right')) return 0;
  if (name.includes('west') || name.includes('left')) return Math.PI;
  return -Math.PI / 2 + index * Math.PI / 2;
}

function addSeatMeshes(THREE) {
  state.seatMeshes.clear();
  const seatMaterial = () => new THREE.MeshStandardMaterial({ color: 0x68e1ae, roughness: 0.42, metalness: 0.12, emissive: 0x0c3828, emissiveIntensity: 0.48 });
  state.seats.forEach((seat) => {
    const allOnStand = state.seats.filter((item) => item.standId === seat.standId);
    const index = seat.standSeatIndex;
    const row = Math.floor(index / 8);
    const column = index % 8;
    const span = Math.min(1.15, Math.max(0.72, allOnStand.length * 0.18));
    const fraction = allOnStand.length <= 1 ? 0 : index / (allOnStand.length - 1) - 0.5;
    const angle = standCenter(seat, seat.standIndex) + fraction * span;
    const radiusX = 13 + row * 0.62;
    const radiusZ = 8.15 + row * 0.55;
    const group = new THREE.Group();
    const material = seatMaterial();
    const pad = new THREE.Mesh(new THREE.BoxGeometry(0.46, 0.16, 0.38), material);
    const back = new THREE.Mesh(new THREE.BoxGeometry(0.46, 0.28, 0.1), material);
    back.position.set(0, 0.2, -0.13);
    group.add(pad, back);
    group.position.set(Math.cos(angle) * radiusX, 2.1 + row * 0.26 + (column % 2) * 0.04, Math.sin(angle) * radiusZ);
    group.rotation.y = -angle;
    group.userData.seatId = seat.id;
    group.traverse((object) => { if (object.isMesh) { object.material = object.material.clone(); object.castShadow = true; } });
    state.scene.add(group);
    state.seatMeshes.set(seat.id, group);
    updateSeatColor(seat);
  });
}

function setCamera() {
  if (!state.camera) return;
  state.camera.position.set(
    state.target.x + state.radius * Math.sin(state.phi) * Math.sin(state.theta),
    state.target.y + state.radius * Math.cos(state.phi),
    state.target.z + state.radius * Math.sin(state.phi) * Math.cos(state.theta)
  );
  state.camera.lookAt(state.target.x, state.target.y, state.target.z);
}

function buildScene(THREE) {
  const canvas = $('stadium-canvas');
  const wrap = $('scene-wrap');
  try {
    state.scene = new THREE.Scene();
    state.scene.background = new THREE.Color(0x0b1713);
    state.camera = new THREE.PerspectiveCamera(34, 1, 0.1, 180);
    state.renderer = new THREE.WebGLRenderer({ canvas, antialias: true, alpha: false, powerPreference: 'low-power' });
    state.renderer.setPixelRatio(Math.min(window.devicePixelRatio || 1, 1.7));
    state.renderer.setSize(wrap.clientWidth, wrap.clientHeight, false);
    state.renderer.outputColorSpace = THREE.SRGBColorSpace;
    state.renderer.toneMapping = THREE.ACESFilmicToneMapping;
    state.renderer.toneMappingExposure = 1.2;
    state.scene.add(new THREE.HemisphereLight(0xcaf5df, 0x15251e, 2.2));
    const key = new THREE.DirectionalLight(0xffe5bd, 2.4);
    key.position.set(-9, 18, 11);
    state.scene.add(key);
    const rim = new THREE.PointLight(0x59dca5, 48, 38);
    rim.position.set(0, 8, -8);
    state.scene.add(rim);
    const floor = new THREE.Mesh(new THREE.CircleGeometry(23, 96), new THREE.MeshStandardMaterial({ color: 0x0d211a, roughness: 1 }));
    floor.rotation.x = -Math.PI / 2;
    floor.position.y = -0.35;
    state.scene.add(floor);
    const bandColors = [0x263b32, 0x334b3e, 0x24382f, 0x1c3028];
    for (let i = 0; i < 4; i++) {
      const innerX = 11.0 + i * 1.05, innerZ = 6.65 + i * 0.72;
      addRingTier(THREE, innerX, innerZ, innerX + 1.28, innerZ + 0.94, 0.65 + i * 0.68, 1.33 + i * 0.68,
        new THREE.MeshStandardMaterial({ color: bandColors[i], roughness: 0.79, metalness: 0.1, side: THREE.DoubleSide }));
      for (let row = 1; row <= 5; row++) addEllipseLine(THREE, innerX + row * 0.21, innerZ + row * 0.16, 0.65 + i * 0.68 + row * 0.12, i % 2 ? 0x668879 : 0x8da79a, 0.27);
    }
    addEllipseLine(THREE, 16.7, 10.75, 4.0, 0x58766a, 0.52);
    addEllipseLine(THREE, 17.2, 11.1, 4.35, 0x2f5a47, 0.35);
    addPitch(THREE);
    addSeatMeshes(THREE);
    state.raycaster = new THREE.Raycaster();
    state.raycaster.params.Line.threshold = 0.15;
    setCamera();
    const resize = () => {
      if (!state.renderer || !state.camera) return;
      const width = Math.max(1, wrap.clientWidth), height = Math.max(1, wrap.clientHeight);
      state.camera.aspect = width / height;
      state.camera.updateProjectionMatrix();
      state.renderer.setSize(width, height, false);
    };
    new ResizeObserver(resize).observe(wrap);
    let previous = null;
    let moved = false;
    canvas.addEventListener('pointerdown', (event) => {
      state.pointer = { x: event.clientX, y: event.clientY, lastX: event.clientX, lastY: event.clientY };
      moved = false;
      canvas.setPointerCapture(event.pointerId);
    });
    canvas.addEventListener('pointermove', (event) => {
      if (!state.pointer) return;
      const dx = event.clientX - state.pointer.lastX, dy = event.clientY - state.pointer.lastY;
      if (Math.abs(event.clientX - state.pointer.x) + Math.abs(event.clientY - state.pointer.y) > 4) moved = true;
      state.pointer.lastX = event.clientX;
      state.pointer.lastY = event.clientY;
      if (moved) {
        state.theta -= dx * 0.006;
        state.phi = Math.max(0.58, Math.min(1.48, state.phi + dy * 0.0048));
        setCamera();
      }
    });
    canvas.addEventListener('pointerup', (event) => {
      if (state.pointer && !moved) pickSeat(event);
      state.pointer = null;
      if (canvas.hasPointerCapture(event.pointerId)) canvas.releasePointerCapture(event.pointerId);
    });
    canvas.addEventListener('pointercancel', () => { state.pointer = null; });
    canvas.addEventListener('wheel', (event) => {
      event.preventDefault();
      state.radius = Math.max(20, Math.min(40, state.radius + Math.sign(event.deltaY) * 1.6));
      setCamera();
    }, { passive: false });
    canvas.addEventListener('contextmenu', (event) => event.preventDefault());
    const render = () => { state.renderer.render(state.scene, state.camera); previous = performance.now(); window.requestAnimationFrame(render); };
    window.requestAnimationFrame(render);
    showMessage('Click a glowing seat to select it');
    setTimeout(() => { if ($('scene-message').textContent === 'Click a glowing seat to select it') showMessage(''); }, 3600);
  } catch (error) {
    console.error('3D stadium view unavailable', error);
    state.three = null;
    showMessage('3D view unavailable · use the seat list');
    canvas.style.display = 'none';
  }
}

function pickSeat(event) {
  const canvas = $('stadium-canvas');
  const rect = canvas.getBoundingClientRect();
  const pointer = new state.three.Vector2(((event.clientX - rect.left) / rect.width) * 2 - 1, -((event.clientY - rect.top) / rect.height) * 2 + 1);
  state.raycaster.setFromCamera(pointer, state.camera);
  const hits = state.raycaster.intersectObjects(state.scene.children, true);
  const hit = hits.find((item) => {
    let object = item.object;
    while (object && !object.userData.seatId) object = object.parent;
    return Boolean(object && object.userData.seatId);
  });
  if (!hit) return;
  let object = hit.object;
  while (object && !object.userData.seatId) object = object.parent;
  if (object) toggleSeat(object.userData.seatId);
}

async function loadStadium() {
  $('refresh-button').disabled = true;
  showMessage('Loading stadium inventory…');
  try {
    const response = await fetch('/api/demo/stadium', { headers: { Accept: 'application/json' }, cache: 'no-store' });
    if (!response.ok) throw new Error('Stadium API returned ' + response.status);
    const data = await response.json();
    state.selected.clear();
    setEventDetails(data);
    renderSeatList();
    if (state.three && state.scene) {
      const oldSeats = [];
      state.scene.traverse((object) => { if (object.userData && object.userData.seatId) oldSeats.push(object); });
      oldSeats.forEach((object) => state.scene.remove(object));
      addSeatMeshes(state.three);
    }
    showMessage(state.three ? '' : '3D library unavailable · seat list remains usable');
  } catch (error) {
    console.error('Could not load stadium inventory', error);
    $('available-count').textContent = 'Stadium service unavailable';
    $('seat-list').replaceChildren();
    const empty = document.createElement('div');
    empty.className = 'empty-state';
    const title = document.createElement('strong');
    title.textContent = 'Could not reach the stadium';
    const note = document.createElement('span');
    note.textContent = 'Start the Java server, then try refreshing.';
    empty.append(title, note);
    $('seat-list').append(empty);
    showMessage('Start the Java server to load seats');
  } finally {
    $('refresh-button').disabled = false;
  }
}

function resetView() {
  state.theta = 0.18;
  state.phi = 1.07;
  state.radius = 29;
  setCamera();
}

$('continue-button').addEventListener('click', () => {
  if (!state.selected.size) showToast('Select at least one available seat first.');
  else showToast('Your preview is ready. Booking and seat holds arrive in Module 5.');
});
$('refresh-button').addEventListener('click', loadStadium);
$('reset-view').addEventListener('click', resetView);

async function start() {
  try {
    state.three = await import('three');
  } catch (error) {
    console.warn('Three.js could not load; continuing with the accessible seat list.', error);
    state.three = null;
    showMessage('3D library unavailable · seat list remains usable');
    $('stadium-canvas').style.display = 'none';
  }
  await loadStadium();
  if (state.three && state.stadium) buildScene(state.three);
  renderSeatList();
}

start();
