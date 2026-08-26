'use strict';
const PptxGenJS = require('pptxgenjs');
const pres = new PptxGenJS();
pres.layout = 'LAYOUT_WIDE';

const NAVY='1A3A5C',DARK='0F1F30',BLUE='065A82',TEAL='1C7293',LTBG='F4F8FC',WHITE='FFFFFF';
const GREEN='15803D',LGRN='DCFCE7',RED='B91C1C',LRED='FEE2E2',ACCENT='0EA5E9',GRAY='64748B';
const LGRAY='E2E8F0',DGRAY='334155',AMBER='D97706',LAMB='FEF3C7',ORANGE='EA580C';

const sh=()=>({type:'outer',color:'000000',blur:5,offset:2,angle:135,opacity:0.10});
function R(sl,x,y,w,h,fill,border,shadow){
  sl.addShape(pres.shapes.RECTANGLE,{x,y,w,h,fill:{color:fill},
    line:border?{color:border,width:1.5}:{color:fill,width:0},shadow:shadow?sh():undefined});
}
function T(sl,text,x,y,w,h,opts){sl.addText(text,{x,y,w,h,margin:0,...opts});}
function ARR(sl,x,y,w,h,color){
  sl.addShape(pres.shapes.RIGHT_ARROW,{x,y,w,h,fill:{color},line:{color,width:0}});
}
function hdr(sl,title,sub){
  R(sl,0,0,13.33,0.9,NAVY);
  T(sl,title,0.4,0.13,10,0.62,{fontSize:24,bold:true,color:WHITE,valign:'middle'});
  T(sl,sub,0.4,0.52,12,0.35,{fontSize:11,color:'A8D8EA'});
}

// ── Slide 1: Title ──────────────────────────────────────────
{
  const sl=pres.addSlide();
  R(sl,0,0,13.33,7.5,DARK);
  R(sl,0,0,0.07,7.5,ACCENT);
  R(sl,1.5,1.5,10.33,4.5,NAVY,undefined,true);
  R(sl,1.5,1.5,10.33,0.04,ACCENT);
  T(sl,'서비스 디스커버리\n& 로드밸런싱',1.5,1.85,10.33,1.95,{fontSize:40,bold:true,color:WHITE,align:'center',valign:'middle',lineSpacingMultiple:1.3});
  T(sl,'상사 질문 대응 · 직접 구현 방안 · 성능 전략',1.5,3.85,10.33,0.5,{fontSize:16,color:'A8D8EA',align:'center'});
  R(sl,3.5,4.5,6.33,0.02,TEAL);
  const tags=[['Consul 자동등록이란?',GREEN,2.5],['Static LB란?',GRAY,1.7],['직접 구현 방안',ACCENT,1.7]];
  let tx=3.5;
  tags.forEach(([t,c,w])=>{
    R(sl,tx,4.68,w,0.32,c);
    T(sl,t,tx,4.68,w,0.32,{fontSize:10,bold:true,color:WHITE,align:'center',valign:'middle'});
    tx+=w+0.2;
  });
  T(sl,'광역 데이터허브 (PINE) — API 관리 플랫폼',1.5,5.45,10.33,0.38,{fontSize:11,color:'6B8CAE',align:'center',italic:true});
}

// ── Slide 2: Static LB vs Dynamic LB ────────────────────────
{
  const sl=pres.addSlide();
  R(sl,0,0,13.33,7.5,LTBG);
  hdr(sl,'Static LB vs Dynamic LB','두 방식 비교 · nginx와의 관계');

  // Left: Static LB
  R(sl,0.3,1.0,6.1,6.2,WHITE,LGRAY,true);
  R(sl,0.3,1.0,6.1,0.45,GRAY);
  T(sl,'Static LB',0.3,1.0,6.1,0.45,{fontSize:18,bold:true,color:WHITE,align:'center',valign:'middle'});
  T(sl,'서버 주소를 설정 파일에 직접 입력',0.5,1.55,5.7,0.35,{fontSize:11,color:DGRAY});

  R(sl,0.5,1.95,5.7,1.75,'1E293B');
  T(sl,'spring:\n  cloud:\n    loadbalancer:\n      instances:\n        data-service:\n          - uri: http://10.0.0.1:8080\n          - uri: http://10.0.0.2:8080',
    0.65,2.0,5.4,1.65,{fontSize:9,color:'A8D8EA',fontFace:'Consolas',lineSpacingMultiple:1.4,valign:'top'});

  R(sl,0.5,3.8,5.7,0.52,LAMB,AMBER);
  T(sl,'nginx upstream 설정과 동일한 개념\n→ 외부 도구 아닌 Spring Cloud 내장 기능',0.65,3.83,5.4,0.46,{fontSize:9.5,color:AMBER,lineSpacingMultiple:1.35});

  T(sl,'한계',0.5,4.43,1.5,0.3,{fontSize:12,bold:true,color:RED});
  [
    '서버 추가/제거 시 파일 수정 + 재시작 필요',
    '죽은 서버 자동 감지 불가 — 헬스체크 없음',
    '자동등록 불가 — 서버 위치를 직접 알아야 함',
  ].forEach((txt,i)=>{
    R(sl,0.5,4.82+i*0.55,0.28,0.28,RED);
    T(sl,'✗',0.5,4.82+i*0.55,0.28,0.28,{fontSize:10,bold:true,color:WHITE,align:'center',valign:'middle'});
    T(sl,txt,0.88,4.82+i*0.55,5.22,0.5,{fontSize:9.5,color:DGRAY,valign:'middle'});
  });

  // Right: Dynamic LB
  R(sl,6.63,1.0,6.4,6.2,WHITE,LGRAY,true);
  R(sl,6.63,1.0,6.4,0.45,GREEN);
  T(sl,'Dynamic LB (서비스 레지스트리)',6.63,1.0,6.4,0.45,{fontSize:15,bold:true,color:WHITE,align:'center',valign:'middle'});
  T(sl,'레지스트리가 서버 목록을 자동으로 관리',6.83,1.55,6.0,0.35,{fontSize:11,color:DGRAY});

  const steps=['서비스 시작','Consul에 자동 등록','헬스체크 자동화','Gateway → 목록 조회 → 라우팅'];
  const scols=[BLUE,GREEN,TEAL,ACCENT];
  steps.forEach((s,i)=>{
    R(sl,6.83,2.0+i*0.88,6.0,0.65,scols[i]);
    T(sl,`${i+1}  ${s}`,6.83,2.0+i*0.88,6.0,0.65,{fontSize:11,bold:true,color:WHITE,align:'center',valign:'middle'});
    if(i<steps.length-1) T(sl,'↓',9.53,2.68+i*0.88,0.8,0.2,{fontSize:12,color:GREEN,align:'center',bold:true});
  });

  T(sl,'장점',6.83,5.6,1.5,0.3,{fontSize:12,bold:true,color:GREEN});
  ['서버 자동 등록 · 자동 해제','헬스체크로 장애 서버 자동 제외','재시작 없이 확장/축소 가능'].forEach((txt,i)=>{
    R(sl,6.83,5.98+i*0.42,0.28,0.28,GREEN);
    T(sl,'✓',6.83,5.98+i*0.42,0.28,0.28,{fontSize:10,bold:true,color:WHITE,align:'center',valign:'middle'});
    T(sl,txt,7.21,5.98+i*0.42,6.02,0.38,{fontSize:9.5,color:DGRAY,valign:'middle'});
  });
}

// ── Slide 3: Consul 자동등록 메커니즘 ───────────────────────
{
  const sl=pres.addSlide();
  R(sl,0,0,13.33,7.5,LTBG);
  hdr(sl,'Consul 자동등록 메커니즘','라이브러리 추가 + 설정 한 줄 → 나머지는 자동');

  const NW=2.1;
  const nodes=[
    {label:'서비스\n시작',col:BLUE,x:0.3},
    {label:'Application\nReadyEvent',col:TEAL,x:2.9},
    {label:'Consul\n등록 API',col:GREEN,x:5.5},
    {label:'Consul\n헬스체크',col:ORANGE,x:8.1},
    {label:'Gateway\n라우팅',col:ACCENT,x:10.7},
  ];
  nodes.forEach((n,i)=>{
    R(sl,n.x,1.1,NW,1.0,n.col,undefined,true);
    T(sl,n.label,n.x,1.1,NW,1.0,{fontSize:12,bold:true,color:WHITE,align:'center',valign:'middle',lineSpacingMultiple:1.3});
    if(i<nodes.length-1) ARR(sl,n.x+NW+0.05,1.42,0.45,0.28,DGRAY);
  });

  const details=[
    'spring-cloud-starter\n-consul-discovery 추가\n설정 한 줄이면 끝',
    '서비스 완전 준비 후\n등록 시작\n(뜨는 중 등록 방지)',
    'PUT /v1/agent/\nservice/register\n자동 호출',
    '10초마다\n/actuator/health\n실패 시 자동 제거',
    'Consul에서 살아있는\n서버 목록 조회\nRound-Robin 분배',
  ];
  nodes.forEach((n,i)=>{
    R(sl,n.x,2.25,NW,1.5,WHITE,LGRAY,true);
    T(sl,details[i],n.x+0.1,2.3,NW-0.2,1.4,{fontSize:9,color:DGRAY,lineSpacingMultiple:1.5,valign:'top'});
  });

  R(sl,0.3,3.9,12.73,0.5,LRED,RED);
  T(sl,'종료 시 →',0.5,3.93,1.8,0.44,{fontSize:11,bold:true,color:RED,valign:'middle'});
  T(sl,'@PreDestroy 실행  →  Consul 해제 요청  →  Gateway 목록에서 자동 제외',2.4,3.93,10.43,0.44,{fontSize:10.5,color:RED,valign:'middle'});

  R(sl,0.3,4.55,12.73,2.7,'1E293B');
  T(sl,'# application.yml — 설정 한 줄로 자동등록 완성',0.5,4.62,12.33,0.3,{fontSize:10,color:'6B8CAE',fontFace:'Consolas'});
  T(sl,'spring:\n  cloud:\n    consul:\n      host: consul-server      # Consul 서버 주소\n      port: 8500\n      discovery:\n        service-name: data-service    # Gateway가 이 이름으로 찾음\n        health-check-path: /actuator/health\n        health-check-interval: 10s',
    0.5,4.97,12.33,2.2,{fontSize:10,color:'A8D8EA',fontFace:'Consolas',lineSpacingMultiple:1.4,valign:'top'});
}

// ── Slide 4: Consul 한계 → 직접 구현 ───────────────────────
{
  const sl=pres.addSlide();
  R(sl,0,0,13.33,7.5,LTBG);
  hdr(sl,'Consul 도입의 한계','우리 조건에서 직접 구현을 선택한 이유');

  const problems=[
    {title:'라이선스',sub:'BSL 1.1 — 상업적 제한',detail:'2023년 8월\nApache 2.0 → BSL 1.1 변경\n\n상업적 제품화 시\n법무 검토 필요',col:RED,lcol:LRED},
    {title:'내장 UI',sub:'별도 관리 화면 존재',detail:'우리가 만든 콘솔 UI 외에\nConsul 자체 UI 별도 존재\n\n관리 포인트 분산\n통합 운영 불가',col:AMBER,lcol:LAMB},
    {title:'별도 서버 / DB',sub:'외부 인프라 추가 필요',detail:'Consul 서버 별도 운영\n자체 Raft 스토리지 사용\n\n운영 복잡도 증가\n장애 포인트 추가',col:ORANGE,lcol:LAMB},
  ];
  problems.forEach((p,i)=>{
    const x=0.3+i*4.35;
    R(sl,x,1.05,4.05,4.7,p.lcol,p.col,true);
    R(sl,x,1.05,4.05,0.5,p.col);
    T(sl,p.title,x,1.05,4.05,0.5,{fontSize:18,bold:true,color:WHITE,align:'center',valign:'middle'});
    T(sl,p.sub,x+0.15,1.62,3.75,0.32,{fontSize:11,color:p.col,bold:true});
    T(sl,p.detail,x+0.15,2.0,3.75,3.55,{fontSize:11,color:DGRAY,lineSpacingMultiple:1.7,valign:'top'});
  });

  T(sl,'↓',6.17,5.85,1.0,0.45,{fontSize:28,color:NAVY,align:'center',bold:true});
  R(sl,2.0,6.38,9.33,0.88,NAVY,undefined,true);
  R(sl,2.0,6.38,0.06,0.88,ACCENT);
  T(sl,'세 조건 모두 충족 → 직접 구현 (Gateway 내장) 으로 전환',2.15,6.38,9.18,0.88,{fontSize:17,bold:true,color:WHITE,valign:'middle'});
}

// ── Slide 5: 직접 구현 구조 ─────────────────────────────────
{
  const sl=pres.addSlide();
  R(sl,0,0,13.33,7.5,LTBG);
  hdr(sl,'직접 구현 구조','Gateway 내장 · 외부 도구 없음 · 기존 DB + Redis 활용');

  // Services (left)
  ['서비스 A','서비스 B','서비스 C'].forEach((s,i)=>{
    R(sl,0.3,1.05+i*1.5,3.0,1.25,WHITE,LGRAY,true);
    R(sl,0.3,1.05+i*1.5,3.0,0.38,NAVY);
    T(sl,s,0.3,1.05+i*1.5,3.0,0.38,{fontSize:12,bold:true,color:WHITE,align:'center',valign:'middle'});
    T(sl,'ApplicationReadyEvent → 자동 등록\n@PreDestroy → 자동 해제\nHeartbeat → 생존 신호',0.45,1.48+i*1.5,2.7,0.77,{fontSize:8.5,color:DGRAY,lineSpacingMultiple:1.4,valign:'top'});
    ARR(sl,3.35,1.55+i*1.5,0.7,0.28,GREEN);
  });

  // Gateway (center)
  R(sl,4.1,1.05,5.1,5.2,WHITE,ACCENT,true);
  R(sl,4.1,1.05,5.1,0.45,ACCENT);
  T(sl,'API Gateway (Registry 내장)',4.1,1.05,5.1,0.45,{fontSize:13,bold:true,color:WHITE,align:'center',valign:'middle'});

  [
    {label:'자동 등록 수신',sub:'REST API 제공 · 서비스 목록 DB 저장',col:GREEN},
    {label:'헬스체크',sub:'Heartbeat + Active Polling\n연속 실패 시 자동 제외',col:TEAL},
    {label:'로드밸런싱',sub:'DB/Redis에서 목록 조회\nRound-Robin 분배',col:BLUE},
    {label:'자동 복구',sub:'헬스체크 재개 시\n목록 자동 복귀',col:ORANGE},
  ].forEach((m,i)=>{
    R(sl,4.25,1.6+i*1.1,4.8,0.9,m.col);
    T(sl,`${m.label}\n${m.sub}`,4.25,1.6+i*1.1,4.8,0.9,{fontSize:10,color:WHITE,align:'center',valign:'middle',lineSpacingMultiple:1.35});
  });

  ARR(sl,9.25,2.1,0.42,0.28,DGRAY);
  ARR(sl,9.25,4.5,0.42,0.28,DGRAY);

  // Storage (right)
  R(sl,9.72,1.05,3.31,2.4,WHITE,LGRAY,true);
  R(sl,9.72,1.05,3.31,0.4,DGRAY);
  T(sl,'Database',9.72,1.05,3.31,0.4,{fontSize:13,bold:true,color:WHITE,align:'center',valign:'middle'});
  ['영속성 완전 보장 (Source of Truth)','서비스 목록 · 라우팅 룰 원본','재시작 후 자동 복구'].forEach((t,i)=>{
    T(sl,`• ${t}`,9.87,1.52+i*0.55,3.01,0.5,{fontSize:9.5,color:DGRAY,valign:'middle'});
  });

  R(sl,9.72,3.55,3.31,2.4,WHITE,LGRAY,true);
  R(sl,9.72,3.55,3.31,0.4,RED);
  T(sl,'Redis (캐시)',9.72,3.55,3.31,0.4,{fontSize:13,bold:true,color:WHITE,align:'center',valign:'middle'});
  ['요청마다 DB 조회 생략','라우팅 룰 · 서비스 목록 캐시','날아가도 DB에서 복구'].forEach((t,i)=>{
    T(sl,`• ${t}`,9.87,4.02+i*0.55,3.01,0.5,{fontSize:9.5,color:DGRAY,valign:'middle'});
  });

  // Health check bar
  R(sl,0.3,6.35,12.73,0.95,WHITE,LGRAY,true);
  R(sl,0.3,6.35,12.73,0.35,TEAL);
  T(sl,'헬스체크 메커니즘',0.3,6.35,12.73,0.35,{fontSize:12,bold:true,color:WHITE,align:'center',valign:'middle'});
  ['Heartbeat (10초)\n서비스 → Gateway 생존 신호','Active Polling (15초)\nGateway → /actuator/health','연속 3회 실패\n→ 자동 제외 (오탐 방지)','헬스체크 재개\n→ 자동 복귀'].forEach((h,i)=>{
    T(sl,h,0.38+i*3.18,6.72,3.1,0.55,{fontSize:9.5,color:DGRAY,align:'center',lineSpacingMultiple:1.3,valign:'middle'});
  });
}

// ── Slide 6: Consul vs 직접 구현 비교표 ─────────────────────
{
  const sl=pres.addSlide();
  R(sl,0,0,13.33,7.5,LTBG);
  hdr(sl,'Consul vs 직접 구현 — 전체 비교','라이선스 · 인프라 · 헬스체크 포함');

  R(sl,0.3,1.0,12.73,0.48,NAVY);
  T(sl,'비교 항목',0.38,1.0,3.27,0.48,{fontSize:11,bold:true,color:WHITE,valign:'middle'});
  T(sl,'Consul',3.72,1.0,4.28,0.48,{fontSize:11,bold:true,color:WHITE,align:'center',valign:'middle'});
  R(sl,8.1,1.0,4.93,0.48,GREEN);
  T(sl,'직접 구현 ✓',8.1,1.0,4.93,0.48,{fontSize:13,bold:true,color:WHITE,align:'center',valign:'middle'});

  const rows=[
    {label:'라이선스',consul:'BSL 1.1 — 상업 제한 ✗',custom:'Apache 2.0 — 완전 자유 ✓',good:true},
    {label:'별도 서버',consul:'Consul 서버 별도 운영 ✗',custom:'없음 — Gateway에 내장 ✓',good:true},
    {label:'내장 UI',consul:'있음 — 관리 포인트 분산 ✗',custom:'없음 — 우리 콘솔 통합 ✓',good:true},
    {label:'영속성',consul:'자체 Raft 스토리지 △',custom:'기존 DB — 완전 보장 ✓',good:true},
    {label:'헬스체크 방식',consul:'HTTP/TCP/Script 선택 ✓',custom:'Heartbeat + Active Polling ✓',good:true},
    {label:'헬스체크 판단',consul:'1회 실패 즉시 DOWN △',custom:'연속 N회 실패 DOWN (커스텀) ✓',good:true},
    {label:'헬스체크 대상',consul:'서버 HTTP 응답 여부 △',custom:'/actuator/health — DB·디스크 포함 ✓',good:true},
    {label:'구현 비용',consul:'설정만으로 즉시 사용 ✓',custom:'직접 개발 필요 △',good:false},
    {label:'검증 안정성',consul:'프로덕션 검증 완료 ✓',custom:'직접 검증 필요 △',good:false},
  ];

  rows.forEach((row,i)=>{
    const y=1.53+i*0.61;
    const bg=i%2===0?WHITE:'F8FAFC';
    R(sl,0.3,y,12.73,0.56,bg,LGRAY);
    T(sl,row.label,0.38,y,3.27,0.56,{fontSize:10,color:NAVY,valign:'middle',bold:true});
    const cc=row.consul.includes('✓')?GREEN:row.consul.includes('✗')?RED:AMBER;
    T(sl,row.consul,3.72,y,4.23,0.56,{fontSize:9.5,color:cc,valign:'middle',align:'center'});
    R(sl,8.15,y+0.05,4.83,0.46,row.good?LGRN:LAMB,row.good?GREEN:AMBER);
    T(sl,row.custom,8.2,y+0.05,4.78,0.46,{fontSize:9.5,bold:row.good,color:row.good?GREEN:AMBER,valign:'middle',align:'center'});
  });
}

// ── Slide 7: DB + Redis 혼합 전략 ───────────────────────────
{
  const sl=pres.addSlide();
  R(sl,0,0,13.33,7.5,LTBG);
  hdr(sl,'DB + Redis 혼합 전략','영속성 + 성능 동시 해결 · 상사 우려 해소');

  R(sl,0.3,1.0,12.73,0.5,LRED,RED);
  T(sl,'문제: DB만 사용 시 매 요청마다 DB 조회 → 고트래픽 시 병목 발생',0.5,1.03,12.33,0.44,{fontSize:11,color:RED,valign:'middle',bold:true});

  // Two role cards
  R(sl,0.3,1.62,5.9,3.25,WHITE,LGRAY,true);
  R(sl,0.3,1.62,5.9,0.45,DGRAY);
  T(sl,'DB — 진실의 원천 (Source of Truth)',0.3,1.62,5.9,0.45,{fontSize:13,bold:true,color:WHITE,align:'center',valign:'middle'});
  ['영속성 완전 보장','Gateway 재시작해도 데이터 유지','서비스 목록 · 라우팅 룰 원본 저장','Redis 장애 시 여기서 복구'].forEach((t,i)=>{
    T(sl,`• ${t}`,0.5,2.15+i*0.54,5.5,0.48,{fontSize:10.5,color:DGRAY,valign:'middle'});
  });

  T(sl,'↔',6.27,2.85,0.79,0.6,{fontSize:28,color:NAVY,align:'center',bold:true});

  R(sl,7.13,1.62,5.9,3.25,WHITE,LGRAY,true);
  R(sl,7.13,1.62,5.9,0.45,RED);
  T(sl,'Redis — 캐시 레이어 (성능)',7.13,1.62,5.9,0.45,{fontSize:13,bold:true,color:WHITE,align:'center',valign:'middle'});
  ['요청마다 DB 조회 생략','라우팅 룰 · 서비스 목록 캐시','변경 시 캐시 무효화 → DB 재조회','날아가도 DB에서 자동 복구'].forEach((t,i)=>{
    T(sl,`• ${t}`,7.33,2.15+i*0.54,5.5,0.48,{fontSize:10.5,color:DGRAY,valign:'middle'});
  });

  // Flow
  R(sl,0.3,5.02,12.73,2.05,WHITE,LGRAY,true);
  T(sl,'요청 처리 흐름',0.5,5.07,3.5,0.3,{fontSize:12,bold:true,color:NAVY});

  // Cache HIT path
  [{label:'클라이언트\n요청',col:BLUE,x:0.5},{label:'Redis\n조회',col:RED,x:2.75},{label:'캐시 HIT\n즉시 반환',col:GREEN,x:5.0}].forEach((f,i,arr)=>{
    R(sl,f.x,5.45,2.1,1.45,f.col,undefined,true);
    T(sl,f.label,f.x,5.45,2.1,1.45,{fontSize:11,bold:true,color:WHITE,align:'center',valign:'middle',lineSpacingMultiple:1.3});
    if(i<arr.length-1) ARR(sl,f.x+2.15,6.02,0.55,0.28,DGRAY);
  });

  // Cache MISS path
  ARR(sl,6.65,6.02,0.5,0.28,AMBER);
  R(sl,7.2,5.45,2.1,1.45,AMBER,undefined,true);
  T(sl,'캐시 MISS\nDB 조회',7.2,5.45,2.1,1.45,{fontSize:11,bold:true,color:WHITE,align:'center',valign:'middle',lineSpacingMultiple:1.3});
  ARR(sl,9.35,6.02,0.35,0.28,DGRAY);
  R(sl,9.75,5.45,3.2,1.45,DGRAY,undefined,true);
  T(sl,'Redis에 저장\n다음 요청부터 HIT',9.75,5.45,3.2,1.45,{fontSize:11,bold:true,color:WHITE,align:'center',valign:'middle',lineSpacingMultiple:1.3});

  // Boss resolution
  R(sl,0.3,7.08,12.73,0.37,LGRN,GREEN);
  T(sl,'상사 우려 해소: "Redis는 캐시일 뿐 — 날아가도 DB 원본이 있어 데이터 손실 없음"',0.5,7.11,12.33,0.3,{fontSize:10.5,color:GREEN,valign:'middle',bold:true});
}

pres.writeFile({fileName:'Discovery_LoadBalancer.pptx'})
  .then(()=>console.log('Done: Discovery_LoadBalancer.pptx'))
  .catch(e=>console.error(e));
