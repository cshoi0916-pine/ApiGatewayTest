'use strict';
const PptxGenJS = require('pptxgenjs');
const pres = new PptxGenJS();
pres.layout = 'LAYOUT_WIDE';

const NAVY  = '1A3A5C'; const DARK  = '0F1F30'; const BLUE  = '065A82';
const TEAL  = '1C7293'; const LTBG  = 'F4F8FC'; const WHITE = 'FFFFFF';
const GREEN = '15803D'; const LGRN  = 'DCFCE7'; const RED   = 'B91C1C';
const LRED  = 'FEE2E2'; const ACCENT= '0EA5E9'; const GRAY  = '64748B';
const LGRAY = 'E2E8F0'; const DGRAY = '334155'; const AMBER = 'D97706';
const LAMB  = 'FEF3C7'; const PURPLE= '6D28D9'; const LPUR  = 'EDE9FE';
const ORANGE= 'EA580C';

const sh = () => ({ type:'outer', color:'000000', blur:5, offset:2, angle:135, opacity:0.10 });
function R(sl, x, y, w, h, fill, border, shadow) {
  sl.addShape(pres.shapes.RECTANGLE, { x, y, w, h, fill:{ color:fill },
    line: border ? { color:border, width:1.5 } : { color:fill, width:0 },
    shadow: shadow ? sh() : undefined });
}
function T(sl, text, x, y, w, h, opts) { sl.addText(text, { x, y, w, h, margin:0, ...opts }); }
function LN(sl, x, y, w, h, color, dash) {
  sl.addShape(pres.shapes.LINE, { x, y, w, h, line:{ color, width:1.5, dashType: dash||'solid' } });
}
function ARR(sl, x, y, w, h, color) {
  sl.addShape(pres.shapes.RIGHT_ARROW, { x, y, w, h, fill:{ color }, line:{ color, width:0 } });
}
function hdr(sl, title, sub) {
  R(sl, 0, 0, 13.33, 0.9, NAVY);
  T(sl, title, 0.4, 0.13, 10, 0.62, { fontSize:24, bold:true, color:WHITE, valign:'middle' });
  T(sl, sub,   0.4, 0.52, 12, 0.35, { fontSize:11, color:'A8D8EA' });
}
function sidebarUI(sl, activeIdx) {
  R(sl, 0.3, 1.4, 1.7, 5.75, '1E293B');
  T(sl, 'APIGW\n콘솔', 0.35, 1.5, 1.6, 0.65, { fontSize:11, bold:true, color:ACCENT, align:'center', valign:'middle' });
  ['서비스','라우팅 룰','API 요청 이력','접속 이력','서킷브레이크'].forEach((m, i) => {
    R(sl, 0.3, 2.22+i*0.88, 1.7, 0.8, i===activeIdx ? NAVY : '1E293B');
    T(sl, m, 0.35, 2.22+i*0.88, 1.6, 0.8, { fontSize:9.5, color: i===activeIdx ? WHITE : '94A3B8', align:'center', valign:'middle' });
  });
}
function browserFrame(sl, url) {
  R(sl, 0.3, 1.05, 12.73, 6.1, WHITE, LGRAY, true);
  R(sl, 0.3, 1.05, 12.73, 0.35, DGRAY);
  ['EE5555','00AA00','FFAA00'].forEach((c, i) => R(sl, 0.48+i*0.28, 1.13, 0.16, 0.16, c));
  T(sl, url, 1.35, 1.1, 10, 0.28, { fontSize:9, color:'AAAAAA', fontFace:'Consolas' });
}

// ══════════════════════════════════════════════════════════
// 1. 타이틀
// ══════════════════════════════════════════════════════════
{
  const sl = pres.addSlide();
  R(sl, 0, 0, 13.33, 7.5, DARK);
  R(sl, 0, 0, 0.07, 7.5, ACCENT);
  R(sl, 1.8, 1.6, 9.73, 4.3, NAVY, undefined, true);
  R(sl, 1.8, 1.6, 9.73, 0.04, ACCENT);
  T(sl, 'API Gateway', 1.8, 2.0, 9.73, 1.3, { fontSize:52, bold:true, color:WHITE, align:'center', valign:'middle' });
  T(sl, 'Spring Cloud Gateway 기반 단일 진입점 아키텍처', 1.8, 3.25, 9.73, 0.55, { fontSize:18, color:'A8D8EA', align:'center', valign:'middle' });
  R(sl, 3.8, 3.95, 5.73, 0.02, TEAL);
  const tags = ['Spring Cloud Gateway','Consul'];
  const tcols = [TEAL, GREEN];
  let tx = 4.2;
  tags.forEach((t, i) => {
    const w = t.length * 0.09 + 0.3;
    R(sl, tx, 4.2, w, 0.28, tcols[i]);
    T(sl, t, tx, 4.2, w, 0.28, { fontSize:10, bold:true, color:WHITE, align:'center', valign:'middle' });
    tx += w + 0.15;
  });
  T(sl, '광역 데이터허브 (PINE) — API 관리 플랫폼', 1.8, 5.1, 9.73, 0.4, { fontSize:11, color:'6B8CAE', align:'center', italic:true });
}

// ══════════════════════════════════════════════════════════
// 2. 전체 아키텍처
// ══════════════════════════════════════════════════════════
{
  const sl = pres.addSlide();
  R(sl, 0, 0, 13.33, 7.5, LTBG);
  hdr(sl, '전체 아키텍처', 'Client → Gateway → 6개 백엔드 서비스 · 광역 데이터허브 (PINE)');

  // Clients
  R(sl, 0.2, 1.05, 1.6, 3.6, NAVY, undefined, true);
  R(sl, 0.2, 1.05, 1.6, 0.38, BLUE);
  T(sl, 'Clients', 0.2, 1.05, 1.6, 0.38, { fontSize:10, bold:true, color:WHITE, align:'center', valign:'middle' });
  ['웹 브라우저','모바일 앱','외부 API','파트너사'].forEach((c, i) => {
    R(sl, 0.3, 1.52+i*0.7, 1.4, 0.55, WHITE, LGRAY);
    T(sl, c, 0.3, 1.52+i*0.7, 1.4, 0.55, { fontSize:9.5, color:DGRAY, align:'center', valign:'middle' });
  });
  T(sl, ':28081', 0.3, 3.7, 1.4, 0.28, { fontSize:9, color:ACCENT, align:'center', bold:true });
  ARR(sl, 1.85, 2.65, 0.55, 0.3, ACCENT);

  // Gateway
  R(sl, 2.5, 1.05, 2.6, 5.35, WHITE, ACCENT, true);
  R(sl, 2.5, 1.05, 2.6, 0.42, ACCENT);
  T(sl, 'API Gateway (DH-APIGW)', 2.5, 1.05, 2.6, 0.42, { fontSize:9.5, bold:true, color:WHITE, align:'center', valign:'middle' });
  T(sl, 'apigw:28081', 2.6, 1.52, 2.4, 0.25, { fontSize:8.5, color:ACCENT, align:'center', bold:true, fontFace:'Consolas' });
  [['단일 진입점',TEAL],['라우팅 룰 관리',BLUE],['로드밸런싱 (Consul)',GREEN],['무중단 동적 라우팅',ORANGE]].forEach(([t,c], i) => {
    R(sl, 2.6, 1.88+i*0.82, 2.38, 0.68, c);
    T(sl, t, 2.6, 1.88+i*0.82, 2.38, 0.68, { fontSize:10, bold:true, color:WHITE, align:'center', valign:'middle' });
  });
  ARR(sl, 5.2, 2.85, 0.55, 0.3, ACCENT);

  // Backend Services
  const svcs = [
    {id:'DH-APIGW',              addr:'apigw:28081',          col:ACCENT},
    {id:'DH-DATAINGEST',         addr:'dataingest',            col:TEAL},
    {id:'DH-DATAMANAGER',        addr:'datamanager:8080',      col:BLUE},
    {id:'DH-DATASERVICE',        addr:'dataservice:18082',     col:GREEN},
    {id:'DH-DATASERVICEBROKER',  addr:'dataservicebroker:8082',col:ORANGE},
    {id:'DH-INGESTINTERFACE',    addr:'ingestinterface:8084',  col:PURPLE},
  ];
  R(sl, 5.85, 1.05, 3.8, 5.35, WHITE, LGRAY, true);
  R(sl, 5.85, 1.05, 3.8, 0.38, DGRAY);
  T(sl, 'Backend Services', 5.85, 1.05, 3.8, 0.38, { fontSize:10, bold:true, color:WHITE, align:'center', valign:'middle' });
  svcs.forEach((s, i) => {
    R(sl, 5.95, 1.52+i*0.78, 3.6, 0.68, LTBG, s.col);
    R(sl, 5.95, 1.52+i*0.78, 0.1, 0.68, s.col);
    T(sl, s.id, 6.12, 1.56+i*0.78, 3.35, 0.28, { fontSize:8.5, bold:true, color:NAVY });
    T(sl, s.addr, 6.12, 1.84+i*0.78, 3.35, 0.24, { fontSize:8, color:GRAY, fontFace:'Consolas' });
  });

  // Consul
  R(sl, 9.9, 1.05, 3.2, 2.2, LGRN, GREEN, true);
  R(sl, 9.9, 1.05, 3.2, 0.42, GREEN);
  T(sl, 'Consul', 9.9, 1.05, 3.2, 0.42, { fontSize:12, bold:true, color:WHITE, align:'center', valign:'middle' });
  T(sl, 'Service Discovery', 9.9, 1.55, 3.2, 0.38, { fontSize:10, bold:true, color:GREEN, align:'center' });
  T(sl, '서비스 자동 등록/해제\n헬스체크 & 장애 자동 제외\nRound-Robin 로드밸런싱', 10.05, 1.97, 2.9, 1.18, { fontSize:9.5, color:DGRAY, lineSpacingMultiple:1.5 });

  // 관리 콘솔 UI
  R(sl, 9.9, 3.45, 3.2, 2.95, WHITE, TEAL, true);
  R(sl, 9.9, 3.45, 3.2, 0.38, TEAL);
  T(sl, '관리 콘솔 UI', 9.9, 3.45, 3.2, 0.38, { fontSize:11, bold:true, color:WHITE, align:'center', valign:'middle' });
  T(sl, '권한자 전용', 9.9, 3.88, 3.2, 0.28, { fontSize:9.5, bold:true, color:TEAL, align:'center' });
  ['로그인','서비스 관리','라우팅 룰 관리','이력 조회'].forEach((m, i) => {
    R(sl, 10.0, 4.22+i*0.52, 3.0, 0.42, LTBG, TEAL);
    T(sl, m, 10.0, 4.22+i*0.52, 3.0, 0.42, { fontSize:9.5, color:NAVY, align:'center', valign:'middle', bold:true });
  });

  // 하단 설명
  R(sl, 0.2, 6.5, 9.55, 0.72, WHITE, LGRAY, true);
  T(sl, 'Client → :28081 단일 진입 → Gateway 라우팅 룰 매칭 → Consul에서 서비스 위치 조회 → 백엔드 전달', 0.38, 6.58, 9.2, 0.55, { fontSize:9.5, color:DGRAY, valign:'middle' });
}

// ══════════════════════════════════════════════════════════
// 3. 단일 진입점
// ══════════════════════════════════════════════════════════
{
  const sl = pres.addSlide();
  R(sl, 0, 0, 13.33, 7.5, LTBG);
  hdr(sl, '단일 진입점', 'Single Entry Point — 모든 요청이 Gateway를 통과');

  R(sl, 0.3, 1.05, 5.9, 5.95, WHITE, LGRAY, true);
  R(sl, 0.3, 1.05, 5.9, 0.42, RED);
  T(sl, 'BEFORE  — Gateway 없음', 0.3, 1.05, 5.9, 0.42, { fontSize:13, bold:true, color:WHITE, align:'center', valign:'middle' });
  ['Web','Mobile','API','파트너'].forEach((c, i) => {
    R(sl, 0.5, 1.65+i*0.75, 1.1, 0.52, LRED, RED);
    T(sl, c, 0.5, 1.65+i*0.75, 1.1, 0.52, { fontSize:10, color:RED, align:'center', valign:'middle', bold:true });
  });
  ['DATAINGEST','DATAMANAGER','DATASERVICE'].forEach((s, i) => {
    R(sl, 4.6, 1.92+i*1.2, 1.45, 0.7, 'FFF0F0', RED);
    T(sl, s, 4.6, 1.92+i*1.2, 1.45, 0.7, { fontSize:8.5, color:RED, align:'center', valign:'middle' });
  });
  [[1.91,2.23],[1.91,3.23],[1.91,4.23],[2.66,2.23],[2.66,3.23],[2.66,4.23],[3.41,2.23],[3.41,3.23],[3.41,4.23],[4.16,2.23],[4.16,3.23],[4.16,4.23]].forEach(([sy,ey]) => {
    LN(sl, 1.65, sy+0.26, 4.6-1.65, ey-sy-0.1, 'E57373');
  });
  T(sl, '문제점', 0.5, 4.82, 5.5, 0.3, { fontSize:10, bold:true, color:RED });
  ['각 서비스마다 포트 개별 관리','URL 패턴 파편화','인증 로직 중복 구현','서비스 추가 시 클라이언트 수정'].forEach((p, i) => {
    T(sl, '✗  '+p, 0.5, 5.12+i*0.32, 5.5, 0.3, { fontSize:9.5, color:'B91C1C' });
  });

  R(sl, 6.85, 1.05, 6.15, 5.95, WHITE, LGRAY, true);
  R(sl, 6.85, 1.05, 6.15, 0.42, GREEN);
  T(sl, 'AFTER  — API Gateway 적용', 6.85, 1.05, 6.15, 0.42, { fontSize:13, bold:true, color:WHITE, align:'center', valign:'middle' });
  ['Web','Mobile','API','파트너'].forEach((c, i) => {
    R(sl, 7.05, 1.65+i*0.75, 1.1, 0.52, LGRN, GREEN);
    T(sl, c, 7.05, 1.65+i*0.75, 1.1, 0.52, { fontSize:10, color:GREEN, align:'center', valign:'middle', bold:true });
    ARR(sl, 8.2, 1.8+i*0.75, 0.8, 0.26, ACCENT);
  });
  R(sl, 9.1, 2.25, 1.5, 2.1, NAVY, ACCENT, true);
  T(sl, 'API\nGateway\n:28081', 9.1, 2.25, 1.5, 2.1, { fontSize:11, bold:true, color:WHITE, align:'center', valign:'middle' });
  ['DATAINGEST','DATAMANAGER','DATASERVICE'].forEach((s, i) => {
    R(sl, 11.0, 1.92+i*1.2, 1.7, 0.7, LGRN, GREEN);
    T(sl, s, 11.0, 1.92+i*1.2, 1.7, 0.7, { fontSize:8.5, color:GREEN, align:'center', valign:'middle' });
    ARR(sl, 10.65, 2.12+i*1.2, 0.3, 0.26, GREEN);
  });
  T(sl, '효과', 7.05, 4.82, 5.7, 0.3, { fontSize:10, bold:true, color:GREEN });
  ['단일 포트(28081) 진입점','URL 패턴 통합 관리','서비스 추가 시 Gateway만 수정','인증/토큰 검증 중앙 처리'].forEach((b, i) => {
    T(sl, '✓  '+b, 7.05, 5.12+i*0.32, 5.7, 0.3, { fontSize:9.5, color:'15803D' });
  });
}

// ══════════════════════════════════════════════════════════
// 4. 라우팅 룰 & 서비스 구성
// ══════════════════════════════════════════════════════════
{
  const sl = pres.addSlide();
  R(sl, 0, 0, 13.33, 7.5, LTBG);
  hdr(sl, '라우팅 룰 & 서비스 구성', '실제 등록된 서비스 6개 · 라우팅 룰 DB 관리');

  R(sl, 0.3, 1.05, 7.9, 5.95, WHITE, LGRAY, true);
  T(sl, '등록된 라우팅 룰 (실제)', 0.5, 1.12, 5, 0.35, { fontSize:12, bold:true, color:NAVY });
  R(sl, 0.35, 1.52, 7.8, 0.35, NAVY);
  [['서비스',0.4,2.05],['API 경로',2.5,4.25],['메소드',6.8,1.2]].forEach(([h,x,w]) => {
    T(sl, h, x, 1.52, w, 0.35, { fontSize:9, bold:true, color:WHITE, valign:'middle' });
  });
  [{svc:'DH-APIGW', path:'/internal/token', method:'-', col:AMBER},
   {svc:'DH-DATAINGEST', path:'/api/ingest/{dataModelId}', method:'POST', col:TEAL},
   {svc:'DH-DATAMANAGER', path:'/datamodels', method:'GET', col:BLUE},
   {svc:'DH-DATAMANAGER', path:'/datamodels/{datamodelId}', method:'GET', col:BLUE},
   {svc:'DH-DATAMANAGER', path:'/datasets', method:'GET', col:BLUE},
   {svc:'DH-DATAMANAGER', path:'/datasets/{datasetId}', method:'GET', col:BLUE},
   {svc:'DH-DATASERVICE', path:'/entities', method:'GET', col:GREEN},
   {svc:'DH-DATASERVICE', path:'/entities/{id}', method:'GET', col:GREEN},
   {svc:'DH-DATASERVICE', path:'/subscriptions', method:'GET/POST', col:GREEN},
   {svc:'DH-DATASERVICE', path:'/subscriptions/{subscriptionId}', method:'PATCH/DEL', col:GREEN},
   {svc:'DH-DATASERVICE', path:'/temporal/entities', method:'GET', col:GREEN},
   {svc:'DH-DATASERVICE', path:'/types', method:'GET', col:GREEN},
  ].forEach((r, i) => {
    R(sl, 0.35, 1.9+i*0.42, 7.8, 0.4, i%2===0 ? WHITE : 'F8FAFC', LGRAY);
    R(sl, 0.35, 1.9+i*0.42, 0.1, 0.4, r.col);
    T(sl, r.svc, 0.5, 1.92+i*0.42, 1.95, 0.36, { fontSize:7.5, color:DGRAY, valign:'middle', fontFace:'Consolas' });
    T(sl, r.path, 2.5, 1.92+i*0.42, 4.2, 0.36, { fontSize:9, color:NAVY, valign:'middle', fontFace:'Consolas' });
    const mCol = r.method==='POST'?GREEN : r.method.includes('PATCH')?AMBER : r.method==='-'?GRAY : TEAL;
    R(sl, 6.8, 1.95+i*0.42, 1.25, 0.28, r.method==='-'?LGRAY : LAMB, mCol);
    T(sl, r.method, 6.8, 1.95+i*0.42, 1.25, 0.28, { fontSize:7.5, bold:true, color:mCol, align:'center', valign:'middle' });
  });
  T(sl, '+ 2페이지 추가 라우팅 룰 존재', 0.5, 6.88, 7.5, 0.25, { fontSize:9, color:GRAY, italic:true });

  R(sl, 8.45, 1.05, 4.55, 2.65, WHITE, LGRAY, true);
  R(sl, 8.45, 1.05, 4.55, 0.38, TEAL);
  T(sl, '서비스 등록 구조', 8.45, 1.05, 4.55, 0.38, { fontSize:11, bold:true, color:WHITE, align:'center', valign:'middle' });
  [['서비스 아이디','DH-DATASERVICE'],['주소','dataservice:18082'],['사용여부','사용 / 미사용'],['관리','콘솔 UI 등록/수정']].forEach(([k,v], i) => {
    T(sl, k, 8.6, 1.52+i*0.48, 1.8, 0.4, { fontSize:9, color:GRAY, valign:'middle' });
    T(sl, v, 10.45, 1.52+i*0.48, 2.4, 0.4, { fontSize:9.5, color:NAVY, valign:'middle', bold:true, fontFace:'Consolas' });
  });

  R(sl, 8.45, 3.85, 4.55, 1.9, WHITE, LGRAY, true);
  R(sl, 8.45, 3.85, 4.55, 0.38, TEAL);
  T(sl, '라우팅 룰 관리 흐름', 8.45, 3.85, 4.55, 0.38, { fontSize:11, bold:true, color:WHITE, align:'center', valign:'middle' });
  [['콘솔 UI에서 등록/수정','관리자가 경로·서비스 지정'],
   ['Gateway 즉시 반영','재시작 없이 0ms 적용'],
   ['Consul 연동','서비스 위치 자동 조회']].forEach(([k,v], i) => {
    R(sl, 8.5, 4.32+i*0.45, 0.22, 0.34, TEAL);
    T(sl, k, 8.8, 4.32+i*0.45, 2.2, 0.34, { fontSize:9, bold:true, color:NAVY, valign:'middle' });
    T(sl, v, 11.05, 4.32+i*0.45, 1.85, 0.34, { fontSize:8.5, color:DGRAY, valign:'middle' });
  });

  R(sl, 8.45, 5.88, 4.55, 1.12, WHITE, LGRAY, true);
  R(sl, 8.45, 5.88, 2.18, 0.42, BLUE);
  T(sl, 'CONFIG', 8.45, 5.88, 2.18, 0.42, { fontSize:10, bold:true, color:WHITE, align:'center', valign:'middle' });
  T(sl, 'yml 정의 · 재시작 필요', 10.68, 5.88, 2.28, 0.42, { fontSize:9, color:DGRAY, valign:'middle' });
  R(sl, 8.45, 6.35, 2.18, 0.42, GREEN);
  T(sl, 'MANUAL', 8.45, 6.35, 2.18, 0.42, { fontSize:10, bold:true, color:WHITE, align:'center', valign:'middle' });
  T(sl, 'API 등록 → Redis\n무중단 즉시 반영', 10.68, 6.35, 2.28, 0.42, { fontSize:9, color:DGRAY, valign:'middle' });
}

// ══════════════════════════════════════════════════════════
// 5. Eureka 대안 비교 (NEW)
// ══════════════════════════════════════════════════════════
{
  const sl = pres.addSlide();
  R(sl, 0, 0, 13.33, 7.5, LTBG);
  hdr(sl, '서비스 디스커버리 — Eureka 대안 비교', 'Eureka 유지보수 중단 → 4가지 대안 검토 → Consul 선택');

  // Eureka 문제점 배너
  R(sl, 0.3, 1.05, 12.73, 0.62, LRED, RED);
  T(sl, '⚠  Netflix Eureka — 2018년 유지보수 중단 · Spring Cloud에서 deprecated 경고 · 장기 운영 리스크', 0.5, 1.05, 12.5, 0.62, { fontSize:11, color:RED, valign:'middle', bold:true });

  // 비교 테이블 헤더
  const cols = [
    { label:'비교 항목', x:0.3,  w:2.5,  header:true },
    { label:'Static LB', x:2.85, w:2.2,  col:GRAY },
    { label:'Zookeeper', x:5.1,  w:2.2,  col:DGRAY },
    { label:'Nacos',     x:7.35, w:2.2,  col:BLUE },
    { label:'Consul ✓',  x:9.6,  w:3.4,  col:GREEN, selected:true },
  ];

  R(sl, 0.3, 1.75, 12.73, 0.48, NAVY);
  cols.forEach(c => {
    const bg = c.selected ? GREEN : c.header ? NAVY : NAVY;
    T(sl, c.label, c.x+0.05, 1.75, c.w-0.08, 0.48, { fontSize: c.selected ? 13 : 11, bold:true, color:WHITE, align:'center', valign:'middle' });
  });

  const rows = [
    { label:'유지보수 상태',  vals:['없음 ✗','활발 △','활발 △','활발 ✓'] },
    { label:'Spring Cloud\n공식 지원', vals:['기본 △','있음 △','공식 ✓','공식 ✓'] },
    { label:'헬스체크 자동화', vals:['없음 ✗','없음 ✗','내장 ✓','내장 ✓'] },
    { label:'KV Store\n(설정 관리)', vals:['없음 ✗','있음 △','있음 ✓','있음 ✓'] },
    { label:'설치/운영 복잡도', vals:['낮음 ✓','높음 ✗','중간 △','낮음 ✓'] },
    { label:'한국 레퍼런스', vals:['많음 ✓','적음 △','중국 중심','풍부 ✓'] },
    { label:'쿠버네티스 불필요', vals:['가능 ✓','가능 ✓','가능 ✓','가능 ✓'] },
  ];

  const rowColors = (val) => {
    if (val.includes('✓')) return GREEN;
    if (val.includes('✗')) return RED;
    return AMBER;
  };
  const rowBgColors = (val) => {
    if (val.includes('✓')) return LGRN;
    if (val.includes('✗')) return LRED;
    return LAMB;
  };

  rows.forEach((row, i) => {
    const y = 2.28 + i * 0.62;
    const bg = i % 2 === 0 ? WHITE : 'F8FAFC';
    R(sl, 0.3, y, 12.73, 0.58, bg, LGRAY);
    T(sl, row.label, 0.38, y, 2.38, 0.58, { fontSize:9.5, color:NAVY, valign:'middle', bold:true });
    const xs = [2.85, 5.1, 7.35, 9.6];
    const ws = [2.2, 2.2, 2.2, 3.4];
    row.vals.forEach((v, j) => {
      const isConsul = j === 3;
      if (isConsul) {
        R(sl, xs[j]+0.1, y+0.08, ws[j]-0.2, 0.42, rowBgColors(v), rowColors(v));
        T(sl, v, xs[j]+0.1, y+0.08, ws[j]-0.2, 0.42, { fontSize:10, bold:true, color:rowColors(v), align:'center', valign:'middle' });
      } else {
        T(sl, v, xs[j]+0.1, y, ws[j]-0.2, 0.58, { fontSize:9.5, color:rowColors(v), align:'center', valign:'middle' });
      }
    });
  });

  // 선택 이유 요약
  R(sl, 9.6, 6.58, 3.4, 0.72, LGRN, GREEN, true);
  T(sl, 'Consul 선택 이유\n낮은 복잡도 · 공식 지원 · 풍부한 레퍼런스 · KV Store 포함', 9.65, 6.58, 3.3, 0.72, { fontSize:9, color:GREEN, valign:'middle', bold:true });
}

// ══════════════════════════════════════════════════════════
// 6. 로드밸런싱 — Consul 동작 흐름
// ══════════════════════════════════════════════════════════
{
  const sl = pres.addSlide();
  R(sl, 0, 0, 13.33, 7.5, LTBG);
  hdr(sl, '로드밸런싱 — Consul 동작 방식', 'Round-Robin 자동 분산 · 헬스체크 · 서비스 자동 등록/해제');

  // 동작 흐름 다이어그램
  R(sl, 0.3, 1.05, 12.73, 3.1, WHITE, LGRAY, true);
  T(sl, 'Consul 로드밸런싱 동작 흐름', 0.5, 1.12, 6, 0.35, { fontSize:12, bold:true, color:NAVY });

  const flow = [
    {label:'클라이언트\n요청', col:BLUE, x:0.45, w:2.0},
    {label:'API Gateway\napigw:28081', col:NAVY, x:2.6, w:2.2},
    {label:'Consul\nRegistry', col:GREEN, x:4.95, w:2.0},
  ];
  flow.forEach((f, i) => {
    R(sl, f.x, 1.6, f.w, 0.95, f.col, undefined, true);
    T(sl, f.label, f.x, 1.6, f.w, 0.95, { fontSize:10, bold:true, color:WHITE, align:'center', valign:'middle' });
    if (i < 2) ARR(sl, f.x+f.w+0.02, 1.9, 0.12, 0.3, ACCENT);
  });

  // 서비스 3개 (Round-Robin)
  const svcFlow = [
    {label:'DH-DATASERVICE\n:18082', col:TEAL, x:7.1},
    {label:'DH-DATAMANAGER\n:8080', col:BLUE, x:9.45},
    {label:'DH-DATAINGEST\n(동적)', col:PURPLE, x:11.65},
  ];
  svcFlow.forEach((s, i) => {
    R(sl, s.x, 1.6, 2.1, 0.95, s.col, undefined, true);
    T(sl, s.label, s.x, 1.6, 2.1, 0.95, { fontSize:9.5, bold:true, color:WHITE, align:'center', valign:'middle' });
  });
  ARR(sl, 7.08, 1.9, 0.1, 0.3, ACCENT);

  // Round-Robin 표시
  LN(sl, 7.15, 2.6, 0, 0.45, TEAL, 'dash');
  LN(sl, 7.15, 3.05, 6.65, 0, TEAL, 'dash');
  LN(sl, 13.8, 2.6, 0, 0.45, TEAL, 'dash');
  T(sl, 'Round-Robin 순차 분산', 9.7, 3.08, 3.5, 0.28, { fontSize:9, color:TEAL, align:'center' });

  // 단계별 설명 (1~4)
  T(sl, '동작 단계', 0.5, 3.3, 4, 0.3, { fontSize:10, bold:true, color:NAVY });
  [
    '① 서비스 시작 → Consul에 자동 등록 (서비스 ID + 주소 + 포트)',
    '② Gateway가 Consul에서 인스턴스 목록 조회',
    '③ Round-Robin으로 요청 순차 분산',
    '④ 헬스체크 실패 인스턴스 자동 제외 — 수동 개입 불필요',
  ].forEach((s, i) => {
    T(sl, s, 0.5, 3.65+i*0.38, 12.5, 0.35, { fontSize:10, color:DGRAY });
  });

  // 장점 카드 3개
  [{title:'자동 등록/해제', desc:'서비스 시작/종료 시\nConsul이 자동 반영'},
   {title:'장애 자동 제외', desc:'헬스체크 실패 시\n라우팅 대상에서 제거'},
   {title:'하드코딩 제거', desc:'IP/포트를 코드에\n직접 명시 불필요'}].forEach((b, i) => {
    R(sl, 0.5+i*4.22, 5.55, 3.8, 1.65, WHITE, GREEN, true);
    R(sl, 0.5+i*4.22, 5.55, 3.8, 0.38, GREEN);
    T(sl, b.title, 0.5+i*4.22, 5.55, 3.8, 0.38, { fontSize:11, bold:true, color:WHITE, align:'center', valign:'middle' });
    T(sl, b.desc, 0.65+i*4.22, 5.97, 3.5, 1.18, { fontSize:10.5, color:DGRAY, valign:'top', lineSpacingMultiple:1.5 });
  });
}

// ══════════════════════════════════════════════════════════
// 7. 무중단 서비스
// ══════════════════════════════════════════════════════════
{
  const sl = pres.addSlide();
  R(sl, 0, 0, 13.33, 7.5, LTBG);
  hdr(sl, '무중단 서비스 (동적 라우팅)', 'RouteDefinitionWriter + RefreshRoutesEvent — 재시작 없이 즉시 반영');

  R(sl, 0.3, 1.05, 5.9, 5.95, WHITE, LGRAY, true);
  R(sl, 0.3, 1.05, 5.9, 0.42, RED);
  T(sl, 'BEFORE  — 재시작 필요', 0.3, 1.05, 5.9, 0.42, { fontSize:13, bold:true, color:WHITE, align:'center', valign:'middle' });
  [{n:'1',t:'application.yml 수정',d:'라우트 설정을 파일에서 직접 편집'},
   {n:'2',t:'서버 재시작',d:'Gateway 전체 프로세스 재시작'},
   {n:'3',t:'다운타임 발생',d:'재시작 중 모든 요청 실패 (수~수십 초)'},
   {n:'4',t:'확인 및 롤백',d:'문제 시 yml 재수정 후 재시작 반복'}].forEach((s, i) => {
    R(sl, 0.45, 1.62+i*1.2, 0.5, 0.5, RED);
    T(sl, s.n, 0.45, 1.62+i*1.2, 0.5, 0.5, { fontSize:16, bold:true, color:WHITE, align:'center', valign:'middle' });
    T(sl, s.t, 1.05, 1.62+i*1.2, 4.95, 0.3, { fontSize:11, bold:true, color:'B91C1C' });
    T(sl, s.d, 1.05, 1.95+i*1.2, 4.95, 0.45, { fontSize:9.5, color:DGRAY });
    if (i < 3) ARR(sl, 0.6, 2.15+i*1.2, 0.2, 0.32, RED);
  });

  R(sl, 7.0, 1.05, 6.0, 5.95, WHITE, LGRAY, true);
  R(sl, 7.0, 1.05, 6.0, 0.42, GREEN);
  T(sl, 'AFTER  — 무중단 즉시 적용', 7.0, 1.05, 6.0, 0.42, { fontSize:13, bold:true, color:WHITE, align:'center', valign:'middle' });
  [{n:'1',t:'콘솔 UI에서 라우트 등록',d:'서비스 아이디 + API 경로 + 메소드 입력'},
   {n:'2',t:'라우팅 정의 즉시 저장',d:'RouteDefinitionWriter → 라우트 정의 영속 저장'},
   {n:'3',t:'RefreshRoutesEvent 발행',d:'ApplicationContext 이벤트 → 즉시 반영'},
   {n:'4',t:'무중단 완료',d:'0ms 다운타임 · 기존 요청 영향 없음'}].forEach((s, i) => {
    R(sl, 7.15, 1.62+i*1.2, 0.5, 0.5, GREEN);
    T(sl, s.n, 7.15, 1.62+i*1.2, 0.5, 0.5, { fontSize:16, bold:true, color:WHITE, align:'center', valign:'middle' });
    T(sl, s.t, 7.75, 1.62+i*1.2, 5.05, 0.3, { fontSize:11, bold:true, color:'15803D' });
    T(sl, s.d, 7.75, 1.95+i*1.2, 5.05, 0.45, { fontSize:9.5, color:DGRAY, fontFace:'Consolas' });
    if (i < 3) ARR(sl, 7.3, 2.15+i*1.2, 0.2, 0.32, GREEN);
  });

  R(sl, 7.1, 5.65, 5.75, 1.12, LTBG, TEAL);
  T(sl, '핵심 기술', 7.2, 5.72, 5.5, 0.28, { fontSize:10, bold:true, color:TEAL });
  [['RouteDefinitionWriter','라우트 추가/삭제 인터페이스'],
   ['RefreshRoutesEvent','라우트 갱신 이벤트 트리거'],
   ['ApplicationReadyEvent','시작 시 등록 라우트 자동 복구']].forEach(([code,desc], i) => {
    T(sl, code, 7.2, 6.05+i*0.3, 2.6, 0.28, { fontSize:9.5, color:NAVY, fontFace:'Consolas', bold:true });
    T(sl, desc, 9.85, 6.05+i*0.3, 2.8, 0.28, { fontSize:9, color:DGRAY });
  });
  R(sl, 6.1, 3.05, 0.8, 0.55, LGRAY);
  T(sl, 'VS', 6.1, 3.05, 0.8, 0.55, { fontSize:16, bold:true, color:NAVY, align:'center', valign:'middle' });
}

// ══════════════════════════════════════════════════════════
// 8. 콘솔 UI — 로그인
// ══════════════════════════════════════════════════════════
{
  const sl = pres.addSlide();
  R(sl, 0, 0, 13.33, 7.5, LTBG);
  hdr(sl, '관리 콘솔 UI — 로그인', '권한 있는 관리자만 접근 가능 · 무단 접근 차단');
  browserFrame(sl, 'http://gateway-console:3000/login');
  sidebarUI(sl, -1);

  // 배경
  R(sl, 2.1, 1.42, 10.85, 5.73, 'F0F4F8', LGRAY);

  // 로그인 카드 (중앙)
  R(sl, 4.35, 1.85, 5.6, 4.8, WHITE, LGRAY, true);
  R(sl, 4.35, 1.85, 5.6, 0.58, NAVY);
  T(sl, 'API Gateway 관리 콘솔', 4.35, 1.85, 5.6, 0.58, { fontSize:13, bold:true, color:WHITE, align:'center', valign:'middle' });
  T(sl, '광역 데이터허브 (PINE)', 4.35, 2.52, 5.6, 0.3, { fontSize:10, color:GRAY, align:'center' });

  T(sl, '관리자 아이디', 4.6, 2.98, 5.1, 0.28, { fontSize:10, color:DGRAY, bold:true });
  R(sl, 4.6, 3.3, 5.1, 0.48, WHITE, LGRAY);
  T(sl, 'pinecni', 4.75, 3.3, 4.8, 0.48, { fontSize:10, color:DGRAY, valign:'middle' });

  T(sl, '비밀번호', 4.6, 3.9, 5.1, 0.28, { fontSize:10, color:DGRAY, bold:true });
  R(sl, 4.6, 4.22, 5.1, 0.48, WHITE, LGRAY);
  T(sl, '••••••••••', 4.75, 4.22, 4.8, 0.48, { fontSize:13, color:DGRAY, valign:'middle' });

  R(sl, 4.6, 4.88, 5.1, 0.52, NAVY);
  T(sl, '로그인', 4.6, 4.88, 5.1, 0.52, { fontSize:13, bold:true, color:WHITE, align:'center', valign:'middle' });
  T(sl, '⚠  권한 없는 접근은 차단됩니다', 4.6, 5.52, 5.1, 0.3, { fontSize:9, color:AMBER, align:'center' });

  // 우측 접근 제어 안내
  R(sl, 10.25, 1.85, 2.55, 4.8, WHITE, TEAL, true);
  R(sl, 10.25, 1.85, 2.55, 0.4, TEAL);
  T(sl, '접근 제어', 10.25, 1.85, 2.55, 0.4, { fontSize:11, bold:true, color:WHITE, align:'center', valign:'middle' });
  [{icon:'🔐', text:'관리자 계정\n로그인 필수'},
   {icon:'📋', text:'서비스 · 라우팅 룰\n등록 · 수정 · 삭제'},
   {icon:'📊', text:'API 요청 이력\n접속 이력 조회'},
  ].forEach((item, i) => {
    R(sl, 10.35, 2.35+i*1.42, 2.35, 1.25, LTBG, LGRAY);
    T(sl, item.icon, 10.35, 2.4+i*1.42, 2.35, 0.45, { fontSize:18, align:'center' });
    T(sl, item.text, 10.35, 2.88+i*1.42, 2.35, 0.68, { fontSize:8.5, color:DGRAY, align:'center', valign:'top', lineSpacingMultiple:1.4 });
  });
}

// ══════════════════════════════════════════════════════════
// 9. 콘솔 UI — 서비스 탭
// ══════════════════════════════════════════════════════════
{
  const sl = pres.addSlide();
  R(sl, 0, 0, 13.33, 7.5, LTBG);
  hdr(sl, '관리 콘솔 UI — 서비스 탭', '백엔드 서비스 등록 · 주소 관리 · 사용 여부 제어');
  browserFrame(sl, 'http://gateway-console:3000/api-gateway/services');
  sidebarUI(sl, 0);

  R(sl, 2.1, 1.42, 10.85, 5.73, WHITE, LGRAY);
  T(sl, '서비스', 2.25, 1.52, 5, 0.38, { fontSize:14, bold:true, color:NAVY });
  R(sl, 11.35, 1.57, 1.5, 0.3, GREEN);
  T(sl, '+ 등록', 11.35, 1.57, 1.5, 0.3, { fontSize:10, bold:true, color:WHITE, align:'center', valign:'middle' });

  R(sl, 2.15, 2.0, 10.75, 0.38, NAVY);
  [['서비스 아이디',2.2,2.45],['서비스 명',4.7,2.45],['주소',7.2,2.35],['사용여부',9.6,1.45],['',11.1,0.75]].forEach(([h,x,w]) => {
    T(sl, h, x, 2.0, w, 0.38, { fontSize:9.5, bold:true, color:WHITE, valign:'middle' });
  });

  [{id:'DH-APIGW', name:'DH-APIGW', addr:'apigw:28081'},
   {id:'DH-DATAINGEST', name:'DH-DATAINGEST', addr:'dataingest'},
   {id:'DH-DATAMANAGER', name:'DH-DATAMANAGER', addr:'datamanager:8080'},
   {id:'DH-DATASERVICE', name:'DH-DATASERVICE', addr:'dataservice:18082'},
   {id:'DH-DATASERVICEBROKER', name:'DH-DATASERVICEBROKER', addr:'dataservicebroker:8082'},
   {id:'DH-INGESTINTERFACE', name:'DH-INGESTINTERFACE', addr:'ingestinterface:8084'},
  ].forEach((s, i) => {
    const bg = i%2===0 ? WHITE : 'F8FAFC';
    R(sl, 2.15, 2.42+i*0.58, 10.75, 0.55, bg, LGRAY);
    T(sl, s.id,   2.2,  2.46+i*0.58, 2.45, 0.47, { fontSize:9.5, color:NAVY, valign:'middle', fontFace:'Consolas' });
    T(sl, s.name, 4.7,  2.46+i*0.58, 2.45, 0.47, { fontSize:9.5, color:DGRAY, valign:'middle' });
    T(sl, s.addr, 7.2,  2.46+i*0.58, 2.35, 0.47, { fontSize:9.5, color:GRAY, valign:'middle', fontFace:'Consolas' });
    R(sl, 9.6, 2.5+i*0.58, 1.35, 0.32, LGRN, GREEN);
    T(sl, '사용', 9.6, 2.5+i*0.58, 1.35, 0.32, { fontSize:9.5, bold:true, color:GREEN, align:'center', valign:'middle' });
    T(sl, '⋮', 11.1, 2.46+i*0.58, 0.75, 0.47, { fontSize:12, color:GRAY, align:'center', valign:'middle' });
  });

  // 페이지네이션
  R(sl, 2.15, 5.92, 10.75, 0.38, 'F8FAFC', LGRAY);
  T(sl, '< 1 >', 2.15, 5.92, 10.75, 0.38, { fontSize:10, color:NAVY, align:'center', valign:'middle' });

  // CRUD 안내 배지
  R(sl, 2.15, 6.38, 10.75, 0.35, LTBG, LGRAY);
  T(sl, '등록 · 수정 · 삭제 가능  |  등록 즉시 Gateway 라우팅 대상에 포함', 2.25, 6.38, 10.55, 0.35, { fontSize:9.5, color:DGRAY, valign:'middle' });

}

// ══════════════════════════════════════════════════════════
// 10. 콘솔 UI — 서비스 추가 (모달)
// ══════════════════════════════════════════════════════════
{
  const sl = pres.addSlide();
  R(sl, 0, 0, 13.33, 7.5, LTBG);
  hdr(sl, '관리 콘솔 UI — 서비스 등록', '서비스 아이디 · 통신 방법 · 주소 입력 후 즉시 Gateway 대상 추가');
  browserFrame(sl, 'http://gateway-console:3000/api-gateway/services');
  sidebarUI(sl, 0);

  // 배경 (목록 흐리게)
  R(sl, 2.1, 1.42, 10.85, 5.73, LGRAY);

  // 모달 박스
  R(sl, 3.35, 1.75, 7.2, 5.3, WHITE, LGRAY, true);
  R(sl, 3.35, 1.75, 7.2, 0.52, NAVY);
  T(sl, '서비스 추가', 3.5, 1.75, 6.7, 0.52, { fontSize:13, bold:true, color:WHITE, valign:'middle' });
  T(sl, '✕', 10.1, 1.75, 0.42, 0.52, { fontSize:14, color:WHITE, align:'center', valign:'middle' });

  // 폼 필드 (2열)
  const fields = [
    ['서비스 아이디', '서비스 아이디를 입력하세요', '서비스 명', '서비스 명을 입력하세요'],
    ['통신 방법', '선택  ▼', '주소', '주소를 입력하세요'],
    ['설명', '설명을 입력하세요', '사용여부', '● 사용'],
  ];
  fields.forEach(([lLabel, lPh, rLabel, rPh], i) => {
    const y = 2.45 + i * 1.3;
    // 왼쪽 필드
    T(sl, lLabel, 3.5, y, 3.3, 0.28, { fontSize:9.5, bold:true, color:DGRAY });
    if (lPh.startsWith('선택')) {
      R(sl, 3.5, y+0.32, 3.3, 0.38, 'FAFAFA', LGRAY);
      T(sl, lPh, 3.58, y+0.32, 3.2, 0.38, { fontSize:9, color:'AAAAAA', valign:'middle' });
    } else {
      R(sl, 3.5, y+0.32, 3.3, 0.38, 'FAFAFA', LGRAY);
      T(sl, lPh, 3.58, y+0.32, 3.2, 0.38, { fontSize:9, color:'CCCCCC', valign:'middle' });
    }
    // 오른쪽 필드
    T(sl, rLabel, 7.1, y, 3.3, 0.28, { fontSize:9.5, bold:true, color:DGRAY });
    if (rPh.startsWith('●')) {
      R(sl, 7.1, y+0.32, 1.4, 0.38, LGRN, GREEN);
      T(sl, '● 사용', 7.1, y+0.32, 1.4, 0.38, { fontSize:9.5, bold:true, color:GREEN, align:'center', valign:'middle' });
    } else {
      R(sl, 7.1, y+0.32, 3.3, 0.38, 'FAFAFA', LGRAY);
      T(sl, rPh, 7.18, y+0.32, 3.2, 0.38, { fontSize:9, color:'CCCCCC', valign:'middle' });
    }
  });

  // 저장 버튼
  R(sl, 9.1, 6.7, 1.3, 0.32, NAVY);
  T(sl, '저장', 9.1, 6.7, 1.3, 0.32, { fontSize:10, bold:true, color:WHITE, align:'center', valign:'middle' });
  R(sl, 7.75, 6.7, 1.25, 0.32, LGRAY, GRAY);
  T(sl, '취소', 7.75, 6.7, 1.25, 0.32, { fontSize:10, color:GRAY, align:'center', valign:'middle' });

  R(sl, 2.15, 6.88, 10.75, 0.35, LTBG, LGRAY);
  T(sl, '저장 즉시 Consul 등록 대상에 포함 · Gateway 라우팅 반영', 2.25, 6.88, 10.55, 0.35, { fontSize:9.5, color:DGRAY, valign:'middle' });
}

// ══════════════════════════════════════════════════════════
// 10. 콘솔 UI — 라우팅 룰 탭
// ══════════════════════════════════════════════════════════
{
  const sl = pres.addSlide();
  R(sl, 0, 0, 13.33, 7.5, LTBG);
  hdr(sl, '관리 콘솔 UI — 라우팅 룰 탭', '서비스별 API 경로 등록 · 무중단 즉시 반영 · 전체 전파');
  browserFrame(sl, 'http://gateway-console:3000/api-gateway/routing-rules');
  sidebarUI(sl, 1);

  R(sl, 2.1, 1.42, 10.85, 5.73, WHITE, LGRAY);
  T(sl, '라우팅 룰', 2.25, 1.52, 5, 0.38, { fontSize:14, bold:true, color:NAVY });
  R(sl, 9.25, 1.57, 1.92, 0.3, AMBER);
  T(sl, '라우팅룰 전파', 9.25, 1.57, 1.92, 0.3, { fontSize:9.5, bold:true, color:WHITE, align:'center', valign:'middle' });
  R(sl, 11.22, 1.57, 1.65, 0.3, GREEN);
  T(sl, '+ 등록', 11.22, 1.57, 1.65, 0.3, { fontSize:9.5, bold:true, color:WHITE, align:'center', valign:'middle' });

  R(sl, 2.15, 1.97, 10.75, 0.36, NAVY);
  [['라우팅 룰 아이디',2.2,2.35],['API 경로',4.6,3.0],['메소드',7.65,1.1],['서비스',8.8,1.95],['사용여부',10.8,1.05],['',11.9,0.95]].forEach(([h,x,w]) => {
    T(sl, h, x, 1.97, w, 0.36, { fontSize:8.5, bold:true, color:WHITE, valign:'middle' });
  });

  [{id:'SVC-APIGW-TOKEN', path:'/internal/token', method:'-', svc:'DH-APIGW'},
   {id:'SVC-DATAINGEST-...', path:'/api/ingest/{dataModelId}', method:'POST', svc:'DH-DATAINGEST'},
   {id:'SVC-DATAMANAG-1', path:'/datamodels', method:'GET', svc:'DH-DATAMANAGER'},
   {id:'SVC-DATAMANAG-2', path:'/datamodels/{datamodelId}', method:'GET', svc:'DH-DATAMANAGER'},
   {id:'SVC-DATAMANAG-3', path:'/datasets', method:'GET', svc:'DH-DATAMANAGER'},
   {id:'SVC-DATASVC-1', path:'/entities', method:'GET', svc:'DH-DATASERVICE'},
   {id:'SVC-DATASVC-2', path:'/subscriptions', method:'GET/POST', svc:'DH-DATASERVICE'},
   {id:'SVC-DATASVC-3', path:'/temporal/entities', method:'GET', svc:'DH-DATASERVICE'},
  ].forEach((r, i) => {
    const bg = i%2===0 ? WHITE : 'F8FAFC';
    R(sl, 2.15, 2.36+i*0.48, 10.75, 0.45, bg, LGRAY);
    T(sl, r.id, 2.2, 2.38+i*0.48, 2.35, 0.41, { fontSize:8, color:DGRAY, valign:'middle', fontFace:'Consolas' });
    T(sl, r.path, 4.6, 2.38+i*0.48, 3.0, 0.41, { fontSize:9, color:NAVY, valign:'middle', fontFace:'Consolas' });
    const mCol = r.method==='POST'?GREEN : r.method.includes('/')?TEAL : r.method==='-'?GRAY : TEAL;
    R(sl, 7.65, 2.41+i*0.48, 1.05, 0.3, r.method==='-'?LGRAY:LGRN, mCol);
    T(sl, r.method, 7.65, 2.41+i*0.48, 1.05, 0.3, { fontSize:7.5, bold:true, color:mCol, align:'center', valign:'middle' });
    T(sl, r.svc, 8.8, 2.38+i*0.48, 1.95, 0.41, { fontSize:7.5, color:DGRAY, valign:'middle', fontFace:'Consolas' });
    R(sl, 10.8, 2.41+i*0.48, 1.0, 0.3, LGRN, GREEN);
    T(sl, '사용', 10.8, 2.41+i*0.48, 1.0, 0.3, { fontSize:8, bold:true, color:GREEN, align:'center', valign:'middle' });
    T(sl, '⋮', 11.9, 2.38+i*0.48, 0.95, 0.41, { fontSize:10, color:GRAY, align:'center', valign:'middle' });
  });

  R(sl, 2.15, 6.25, 10.75, 0.32, 'F8FAFC', LGRAY);
  T(sl, '< 1  2 >', 2.15, 6.25, 6, 0.32, { fontSize:9, color:NAVY, align:'center', valign:'middle' });

  R(sl, 2.15, 6.62, 10.75, 0.52, LTBG, TEAL);
  T(sl, '라우팅룰 전파 버튼: 등록된 모든 MANUAL 라우트를 Gateway에 즉시 반영  |  등록 즉시 저장 → RefreshRoutesEvent 발행 → 무중단 적용', 2.25, 6.62, 10.55, 0.52, { fontSize:9.5, color:DGRAY, valign:'middle' });
}

// ══════════════════════════════════════════════════════════
// 12. 콘솔 UI — 라우팅 룰 추가/수정
// ══════════════════════════════════════════════════════════
{
  const sl = pres.addSlide();
  R(sl, 0, 0, 13.33, 7.5, LTBG);
  hdr(sl, '관리 콘솔 UI — 라우팅 룰 등록/수정', '경로·메소드 설정 후 저장 → 무중단 즉시 반영');
  browserFrame(sl, 'http://gateway-console:3000/api-gateway/routing-rules/edit');
  sidebarUI(sl, 1);

  R(sl, 2.1, 1.42, 10.85, 5.73, WHITE, LGRAY);

  // 상단 버튼
  T(sl, '목록', 11.25, 1.47, 1.6, 0.32, { fontSize:9.5, color:NAVY, align:'right', valign:'middle', bold:true });
  LN(sl, 11.25, 1.63, 1.6, 0, LGRAY);

  // 섹션 헤더 — 라우팅 룰 상세
  R(sl, 2.1, 1.82, 10.85, 0.32, LTBG, LGRAY);
  T(sl, '라우팅 룰 상세', 2.2, 1.82, 5, 0.32, { fontSize:10, bold:true, color:NAVY, valign:'middle' });

  // 왼쪽 컬럼
  const leftFields = [
    ['아이디', 'SVC-APIGW-TOKEN'],
    ['대상 서비스 아이디', 'DH-APIGW  ▼'],
    ['메소드', '-  ▼'],
    ['API 경로', '/internal/token'],
    ['순위', '1'],
  ];
  leftFields.forEach(([label, val], i) => {
    const y = 2.25 + i * 0.7;
    T(sl, label, 2.2, y, 4.8, 0.26, { fontSize:8.5, bold:true, color:DGRAY });
    R(sl, 2.2, y+0.28, 4.8, 0.32, 'FAFAFA', LGRAY);
    T(sl, val, 2.28, y+0.28, 4.65, 0.32, { fontSize:9, color:NAVY, valign:'middle' });
  });

  // 오른쪽 컬럼 (인증 관련 제거)
  const rightFields = [
    ['명', 'SVC-APIGW-TOKEN'],
    ['대상 서비스 포트', '28081'],
    ['사용 여부', '● 사용'],
    ['설명', '토큰 발급 내부 API'],
  ];
  rightFields.forEach(([label, val], i) => {
    const y = 2.25 + i * 0.7;
    T(sl, label, 7.3, y, 5.4, 0.26, { fontSize:8.5, bold:true, color:DGRAY });
    if (val.startsWith('●')) {
      R(sl, 7.3, y+0.28, 1.35, 0.32, LGRN, GREEN);
      T(sl, val, 7.3, y+0.28, 1.35, 0.32, { fontSize:9, bold:true, color:GREEN, align:'center', valign:'middle' });
    } else {
      R(sl, 7.3, y+0.28, 5.4, 0.32, 'FAFAFA', LGRAY);
      T(sl, val, 7.38, y+0.28, 5.25, 0.32, { fontSize:9, color:NAVY, valign:'middle' });
    }
  });

  // 섹션 헤더 — 라우팅 상세 조건
  R(sl, 2.1, 5.18, 10.85, 0.32, 'EEF2F8', LGRAY);
  T(sl, '라우팅 상세 조건', 2.2, 5.18, 5, 0.32, { fontSize:10, bold:true, color:NAVY, valign:'middle' });

  // 조건 행 (4열: 타입 / 오퍼레이션 / 속성명 / 속성값)
  [['타입','Path  ▼'],['오퍼레이션','Match  ▼'],['속성명','/internal/**'],['속성값','']].forEach(([label, val], i) => {
    const x = 2.2 + i * 2.68;
    T(sl, label, x, 5.57, 2.5, 0.24, { fontSize:8.5, bold:true, color:DGRAY });
    R(sl, x, 5.83, 2.5, 0.32, 'FAFAFA', LGRAY);
    T(sl, val, x+0.06, 5.83, 2.4, 0.32, { fontSize:9, color:NAVY, valign:'middle' });
  });

  // 조건 설명
  R(sl, 2.1, 6.22, 10.85, 0.45, LTBG, LGRAY);
  T(sl, 'Path + Match + /internal/** → /internal/ 하위 모든 경로 매칭  |  조건 복수 추가 시 AND 적용', 2.2, 6.22, 10.65, 0.45, { fontSize:9, color:DGRAY, valign:'middle', italic:true });

  // 저장/삭제 버튼
  R(sl, 11.0, 6.8, 1.7, 0.35, NAVY);
  T(sl, '저장', 11.0, 6.8, 1.7, 0.35, { fontSize:10, bold:true, color:WHITE, align:'center', valign:'middle' });
  R(sl, 9.25, 6.8, 1.65, 0.35, LRED, RED);
  T(sl, '삭제', 9.25, 6.8, 1.65, 0.35, { fontSize:10, bold:true, color:RED, align:'center', valign:'middle' });
}

// ══════════════════════════════════════════════════════════
// 13. 콘솔 UI — API 요청 이력 탭 (NEW)
// ══════════════════════════════════════════════════════════
{
  const sl = pres.addSlide();
  R(sl, 0, 0, 13.33, 7.5, LTBG);
  hdr(sl, '관리 콘솔 UI — API 요청 이력 탭', '실시간 API 요청 로그 · 상태코드 · 응답시간 · 라우팅 경로 추적');
  browserFrame(sl, 'http://gateway-console:3000/api-gateway/api-request-history');
  sidebarUI(sl, 2);

  R(sl, 2.1, 1.42, 10.85, 5.73, WHITE, LGRAY);

  // 탭 3개 (API 요청 이력 활성)
  [['API 요청 이력',true],['접속 이력',false],['서킷브레이크 이력',false]].forEach(([t, active], i) => {
    R(sl, 2.15+i*2.6, 1.47, 2.5, 0.38, active?NAVY:LGRAY, active?NAVY:LGRAY);
    T(sl, t, 2.15+i*2.6, 1.47, 2.5, 0.38, { fontSize:9.5, bold:active, color:active?WHITE:GRAY, align:'center', valign:'middle' });
  });

  T(sl, 'API 요청 이력', 2.25, 1.92, 5, 0.35, { fontSize:12, bold:true, color:NAVY });

  // 필터
  R(sl, 8.5, 1.97, 1.5, 0.28, LTBG, LGRAY);
  T(sl, '상태: 전체', 8.55, 1.97, 1.4, 0.28, { fontSize:8.5, color:DGRAY, valign:'middle' });
  R(sl, 10.05, 1.97, 1.5, 0.28, LTBG, LGRAY);
  T(sl, '최근 1시간', 10.1, 1.97, 1.4, 0.28, { fontSize:8.5, color:DGRAY, valign:'middle' });
  R(sl, 11.6, 1.97, 1.45, 0.28, TEAL);
  T(sl, '조회', 11.6, 1.97, 1.45, 0.28, { fontSize:8.5, bold:true, color:WHITE, align:'center', valign:'middle' });

  // 테이블 헤더
  R(sl, 2.15, 2.32, 10.75, 0.32, NAVY);
  [['시각',2.2,1.38],['Method',3.63,0.75],['API 경로',4.43,3.1],['서비스',7.58,1.85],['상태',9.48,0.75],['응답시간',10.28,1.0],['IP',11.33,1.52]].forEach(([h,x,w]) => {
    T(sl, h, x, 2.32, w, 0.32, { fontSize:8.5, bold:true, color:WHITE, valign:'middle' });
  });

  const apiLogs = [
    {time:'14:32:01', method:'GET',    path:'/entities',                    svc:'DH-DATASERVICE',      status:'200', ms:'38ms',   ip:'222.107.32.66'},
    {time:'14:32:00', method:'POST',   path:'/api/ingest/{dataModelId}',    svc:'DH-DATAINGEST',       status:'201', ms:'124ms',  ip:'10.0.1.8'},
    {time:'14:31:58', method:'GET',    path:'/datamodels',                  svc:'DH-DATAMANAGER',      status:'200', ms:'42ms',   ip:'222.107.32.66'},
    {time:'14:31:55', method:'GET',    path:'/subscriptions/{id}',          svc:'DH-DATASERVICE',      status:'200', ms:'67ms',   ip:'10.0.2.3'},
    {time:'14:31:52', method:'PATCH',  path:'/subscriptions/{id}',          svc:'DH-DATASERVICE',      status:'200', ms:'89ms',   ip:'10.0.2.3'},
    {time:'14:31:48', method:'GET',    path:'/temporal/entities',           svc:'DH-DATASERVICE',      status:'500', ms:'—',      ip:'222.107.32.66'},
    {time:'14:31:45', method:'GET',    path:'/datasets',                    svc:'DH-DATAMANAGER',      status:'200', ms:'55ms',   ip:'10.0.1.8'},
    {time:'14:31:40', method:'DELETE', path:'/subscriptions/{id}',          svc:'DH-DATASERVICE',      status:'204', ms:'31ms',   ip:'10.0.2.3'},
  ];

  apiLogs.forEach((l, i) => {
    const isErr = l.status.startsWith('5');
    const bg = isErr ? 'FFF5F5' : i%2===0 ? WHITE : 'F8FAFC';
    R(sl, 2.15, 2.67+i*0.43, 10.75, 0.41, bg, LGRAY);
    T(sl, l.time, 2.2, 2.69+i*0.43, 1.38, 0.37, { fontSize:8.5, color:GRAY, valign:'middle', fontFace:'Consolas' });
    const mCol = l.method==='GET'?TEAL : l.method==='POST'?GREEN : l.method==='DELETE'?RED : AMBER;
    R(sl, 3.63, 2.71+i*0.43, 0.7, 0.28, mCol);
    T(sl, l.method, 3.63, 2.71+i*0.43, 0.7, 0.28, { fontSize:7, bold:true, color:WHITE, align:'center', valign:'middle' });
    T(sl, l.path, 4.43, 2.69+i*0.43, 3.1, 0.37, { fontSize:8.5, color:NAVY, valign:'middle', fontFace:'Consolas' });
    T(sl, l.svc, 7.58, 2.69+i*0.43, 1.85, 0.37, { fontSize:8, color:DGRAY, valign:'middle' });
    const sCol = l.status.startsWith('2')?GREEN : l.status.startsWith('5')?RED : AMBER;
    R(sl, 9.48, 2.71+i*0.43, 0.7, 0.28, l.status.startsWith('2')?LGRN:LRED, sCol);
    T(sl, l.status, 9.48, 2.71+i*0.43, 0.7, 0.28, { fontSize:8.5, bold:true, color:sCol, align:'center', valign:'middle' });
    T(sl, l.ms, 10.28, 2.69+i*0.43, 1.0, 0.37, { fontSize:8.5, color:DGRAY, align:'center', valign:'middle' });
    T(sl, l.ip, 11.33, 2.69+i*0.43, 1.52, 0.37, { fontSize:8, color:GRAY, valign:'middle', fontFace:'Consolas' });
  });

  R(sl, 2.15, 6.13, 10.75, 0.28, 'F8FAFC', LGRAY);
  T(sl, '< 1  2  3 >', 2.15, 6.13, 6, 0.28, { fontSize:9, color:NAVY, align:'center', valign:'middle' });

  R(sl, 2.15, 6.47, 10.75, 0.6, LTBG, LGRAY);
  T(sl, '500 응답은 빨간색으로 강조 표시  |  서비스별 · 상태코드별 필터 지원  |  특정 IP의 요청 패턴 추적 가능', 2.25, 6.47, 10.55, 0.6, { fontSize:9.5, color:DGRAY, valign:'middle' });
}

// ══════════════════════════════════════════════════════════
// 12. 콘솔 UI — 토큰 발급 이력 탭 (원래 11)
// ══════════════════════════════════════════════════════════
{
  const sl = pres.addSlide();
  R(sl, 0, 0, 13.33, 7.5, LTBG);
  hdr(sl, '관리 콘솔 UI — 접속 이력 탭', '콘솔 접속 이력 · API 요청 이력 · 서킷브레이크 이력');
  browserFrame(sl, 'http://gateway-console:3000/api-gateway/access-history');
  sidebarUI(sl, 3);

  R(sl, 2.1, 1.42, 10.85, 5.73, WHITE, LGRAY);

  // 탭 3개
  [['API 요청 이력',false],['접속 이력',true],['서킷브레이크 이력',false]].forEach(([t, active], i) => {
    R(sl, 2.15+i*2.6, 1.47, 2.5, 0.38, active?NAVY:LGRAY, active?NAVY:LGRAY);
    T(sl, t, 2.15+i*2.6, 1.47, 2.5, 0.38, { fontSize:9.5, bold:active, color:active?WHITE:GRAY, align:'center', valign:'middle' });
  });

  T(sl, '접속 이력', 2.25, 1.92, 5, 0.35, { fontSize:12, bold:true, color:NAVY });

  // 테이블 헤더 (토큰 컬럼 제거)
  R(sl, 2.15, 2.35, 10.75, 0.34, NAVY);
  [['순번',2.2,0.55],['접속 일시',2.8,1.85],['아이디',4.7,1.5],['IP',6.25,1.6],['만료 일시',7.9,1.85],['상태',9.8,1.25],['비고',11.1,1.75]].forEach(([h,x,w]) => {
    T(sl, h, x, 2.35, w, 0.34, { fontSize:8.5, bold:true, color:WHITE, valign:'middle' });
  });

  [{no:'50',dt:'2025-12-11 10:41:08',id:'pinecni',ip:'222.107.32.66',exp:'2025-12-11 12:21:08',st:'정상',note:''},
   {no:'49',dt:'2025-12-04 09:11:21',id:'pinecni',ip:'222.107.32.66',exp:'2025-12-11 07:51:21',st:'정상',note:''},
   {no:'48',dt:'2025-12-03 20:48:01',id:'pinecni',ip:'222.107.32.66',exp:'2025-12-04 06:48:01',st:'정상',note:''},
   {no:'47',dt:'2025-12-03 20:34:24',id:'pinecni',ip:'222.107.32.66',exp:'2025-12-04 06:34:24',st:'정상',note:''},
   {no:'46',dt:'2025-12-01 15:03:45',id:'pinecni',ip:'222.107.32.66',exp:'2025-12-02 01:03:45',st:'정상',note:''},
   {no:'45',dt:'2025-11-20 13:48:02',id:'pinecni',ip:'0.0.0.0',      exp:'2025-11-20 23:48:02',st:'정상',note:'내부 접속'},
  ].forEach((r, i) => {
    R(sl, 2.15, 2.72+i*0.48, 10.75, 0.44, i%2===0?WHITE:'F8FAFC', LGRAY);
    T(sl, r.no,   2.2,  2.75+i*0.48, 0.55, 0.38, { fontSize:8.5, color:GRAY, align:'center', valign:'middle' });
    T(sl, r.dt,   2.8,  2.75+i*0.48, 1.85, 0.38, { fontSize:8,   color:DGRAY, valign:'middle', fontFace:'Consolas' });
    T(sl, r.id,   4.7,  2.75+i*0.48, 1.5,  0.38, { fontSize:9,   color:NAVY,  valign:'middle' });
    T(sl, r.ip,   6.25, 2.75+i*0.48, 1.6,  0.38, { fontSize:8,   color:GRAY,  valign:'middle', fontFace:'Consolas' });
    T(sl, r.exp,  7.9,  2.75+i*0.48, 1.85, 0.38, { fontSize:8,   color:DGRAY, valign:'middle', fontFace:'Consolas' });
    R(sl, 9.8,  2.78+i*0.48, 1.15, 0.3, LGRN, GREEN);
    T(sl, r.st,   9.8,  2.78+i*0.48, 1.15, 0.3,  { fontSize:8.5, bold:true, color:GREEN, align:'center', valign:'middle' });
    T(sl, r.note, 11.1, 2.75+i*0.48, 1.7,  0.38, { fontSize:8,   color:GRAY,  valign:'middle', italic:true });
  });

  R(sl, 2.15, 5.65, 10.75, 0.28, 'F8FAFC', LGRAY);
  T(sl, '< 1  2 >', 2.15, 5.65, 6, 0.28, { fontSize:9, color:NAVY, align:'center', valign:'middle' });
  R(sl, 2.15, 6.0, 10.75, 0.52, LTBG, LGRAY);
  T(sl, '관리자 접속 이력 감사(Audit) 목적  |  IP · 접속 일시 · 만료 일시 추적 가능', 2.25, 6.0, 10.55, 0.52, { fontSize:9.5, color:DGRAY, valign:'middle' });
}

// ══════════════════════════════════════════════════════════
// 12. 정리
// ══════════════════════════════════════════════════════════
{
  const sl = pres.addSlide();
  R(sl, 0, 0, 13.33, 7.5, DARK);
  R(sl, 0, 0, 0.07, 7.5, ACCENT);
  T(sl, '정리', 0.6, 0.55, 12, 0.7, { fontSize:38, bold:true, color:WHITE });
  T(sl, 'Spring Cloud Gateway로 달성한 4가지 핵심 가치', 0.6, 1.2, 12, 0.45, { fontSize:16, color:'A8D8EA' });

  [{title:'단일 진입점', icon:'→', desc:'모든 요청이 Gateway(:28081)를 통과\n포트·인증 복잡성 제거\n클라이언트 코드 단순화', col:TEAL},
   {title:'라우팅 룰', icon:'⊞', desc:'6개 서비스 · 라우팅 룰 DB 관리\nCONFIG · MANUAL 타입 분리\n실제 운영 중인 API 경로 매핑', col:BLUE},
   {title:'로드밸런싱', icon:'⟷', desc:'Consul 기반 서비스 디스커버리\nRound-Robin 자동 분산\n장애 인스턴스 자동 제외', col:GREEN},
   {title:'무중단 서비스', icon:'◎', desc:'콘솔 UI에서 라우트 추가/삭제\nRedis 저장 → 즉시 반영\n재시작 없이 0ms 다운타임', col:ACCENT},
  ].forEach((c, i) => {
    const x = 0.5+i*3.1;
    R(sl, x, 2.0, 2.9, 4.2, c.col, undefined, true);
    T(sl, c.icon, x, 2.1, 2.9, 0.65, { fontSize:28, color:WHITE, align:'center' });
    T(sl, c.title, x, 2.75, 2.9, 0.5, { fontSize:15, bold:true, color:WHITE, align:'center' });
    LN(sl, x+0.15, 3.28, 2.6, 0, '4A7A9B');
    T(sl, c.desc, x+0.12, 3.38, 2.65, 2.7, { fontSize:10, color:'E0F0FF', valign:'top', lineSpacingMultiple:1.4 });
  });
  T(sl, '관리 콘솔 UI (로그인·서비스·라우팅룰·이력조회)  ·  Consul  ·  Redis  ·  PostgreSQL  ·  외부 인증 연동 준비 완료', 0.5, 6.45, 12.4, 0.45, { fontSize:10, color:'6B8CAE', align:'center', italic:true });
}

// ══════════════════════════════════════════════════════════
// 13. 부록 — 확장 가능 기능
// ══════════════════════════════════════════════════════════
{
  const sl = pres.addSlide();
  R(sl, 0, 0, 13.33, 7.5, LTBG);
  R(sl, 0, 0, 13.33, 0.9, DGRAY);
  T(sl, '[부록]  확장 가능 기능', 0.4, 0.13, 9, 0.62, { fontSize:22, bold:true, color:WHITE, valign:'middle' });
  T(sl, '이미 구현 완료 · 운영 정책 결정 후 즉시 활성화 가능', 0.4, 0.52, 10, 0.35, { fontSize:11, color:'94A3B8' });
  R(sl, 10.2, 0.2, 2.9, 0.5, GREEN);
  T(sl, '추가 개발 불필요', 10.2, 0.2, 2.9, 0.5, { fontSize:10, bold:true, color:WHITE, align:'center', valign:'middle' });

  // Rate Limiting
  R(sl, 0.3, 1.05, 6.15, 5.95, WHITE, LGRAY, true);
  R(sl, 0.3, 1.05, 6.15, 0.42, AMBER);
  T(sl, 'Rate Limiting', 0.3, 1.05, 6.15, 0.42, { fontSize:13, bold:true, color:WHITE, align:'center', valign:'middle' });
  T(sl, '목적', 0.5, 1.58, 5.8, 0.3, { fontSize:10, bold:true, color:AMBER });
  T(sl, '특정 IP 또는 클라이언트가 단시간에 과도한 요청을 보낼 경우 차단\nDDoS 방어 · 서비스 과부하 방지 · 임계값 초과 시 429 Too Many Requests 반환', 0.5, 1.92, 5.8, 0.78, { fontSize:9.5, color:DGRAY, lineSpacingMultiple:1.4 });
  T(sl, '등급별 제한 기준', 0.5, 2.82, 5.8, 0.3, { fontSize:10, bold:true, color:AMBER });
  [{tier:'내부 시스템', limit:'5,000 req/분', col:TEAL},
   {tier:'파트너 API Key', limit:'1,000 req/분', col:BLUE},
   {tier:'일반 API Key', limit:'300 req/분', col:GREEN},
   {tier:'미인증 IP', limit:'60 req/분', col:AMBER}].forEach((t, i) => {
    R(sl, 0.4, 3.18+i*0.62, 5.95, 0.54, i%2===0?LAMB:WHITE, LGRAY);
    R(sl, 0.4, 3.18+i*0.62, 2.1, 0.54, t.col);
    T(sl, t.tier, 0.4, 3.18+i*0.62, 2.1, 0.54, { fontSize:9.5, bold:true, color:WHITE, align:'center', valign:'middle' });
    T(sl, t.limit, 2.55, 3.18+i*0.62, 3.7, 0.54, { fontSize:12, color:NAVY, valign:'middle', bold:true });
  });
  T(sl, '활성화: rate-limit.enabled: true', 0.5, 5.72, 5.8, 0.3, { fontSize:9, color:GRAY, italic:true, fontFace:'Consolas' });

  // 서킷브레이크
  R(sl, 6.88, 1.05, 6.15, 5.95, WHITE, LGRAY, true);
  R(sl, 6.88, 1.05, 6.15, 0.42, ORANGE);
  T(sl, '서킷브레이크', 6.88, 1.05, 6.15, 0.42, { fontSize:13, bold:true, color:WHITE, align:'center', valign:'middle' });
  T(sl, '목적', 7.08, 1.58, 5.8, 0.3, { fontSize:10, bold:true, color:ORANGE });
  T(sl, '백엔드 서비스 장애 시 Gateway에서 빠른 실패(Fail Fast)로 연쇄 장애 차단\n장애 서비스 자동 격리 → fallback 응답 → 회복 후 자동 복귀', 7.08, 1.92, 5.8, 0.78, { fontSize:9.5, color:DGRAY, lineSpacingMultiple:1.4 });
  T(sl, '상태 전이', 7.08, 2.82, 5.8, 0.3, { fontSize:10, bold:true, color:ORANGE });
  [{state:'CLOSED', desc:'정상 상태 · 모든 요청 통과 · 실패율 누적 감시', col:GREEN},
   {state:'OPEN',   desc:'임계값 초과 · 요청 차단 → fallback 응답 즉시 반환', col:RED},
   {state:'HALF-OPEN', desc:'일부 요청 허용 → 성공 시 CLOSED 복귀', col:AMBER}].forEach((s, i) => {
    R(sl, 7.08, 3.22+i*0.88, 1.65, 0.72, s.col);
    T(sl, s.state, 7.08, 3.22+i*0.88, 1.65, 0.72, { fontSize:9.5, bold:true, color:WHITE, align:'center', valign:'middle' });
    T(sl, s.desc, 8.8, 3.22+i*0.88, 4.1, 0.72, { fontSize:9, color:DGRAY, valign:'middle' });
  });
  T(sl, '콘솔에서 이력 확인', 7.08, 5.98, 5.8, 0.3, { fontSize:10, bold:true, color:ORANGE });
  T(sl, '서킷브레이크 이력 탭: 발동 횟수 · 대상 서비스 · 발생 시각 조회', 7.08, 6.32, 5.8, 0.3, { fontSize:9.5, color:DGRAY });
  T(sl, '활성화: resilience4j 설정 추가', 7.08, 6.72, 5.8, 0.3, { fontSize:9, color:GRAY, italic:true, fontFace:'Consolas' });
}

pres.writeFile({ fileName: 'C:\\dev\\02_work\\ApiGatewayTest\\API_Gateway_v2.pptx' })
  .then(() => console.log('Done: API_Gateway_v2.pptx'))
  .catch(e => { console.error(e); process.exit(1); });
