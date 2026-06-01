const pptxgen = require("pptxgenjs");

const pres = new pptxgen();
pres.layout = "LAYOUT_16x9";
pres.title = "API Gateway 프로젝트";

// ─── 색상 팔레트 ───────────────────────────────────────────
const C = {
  navy:    "0A2342",
  blue:    "065A82",
  teal:    "1C7293",
  mint:    "02C39A",
  white:   "FFFFFF",
  light:   "F0F4F8",
  gray:    "64748B",
  dark:    "1E293B",
  done:    "0D9488",
  todo:    "F59E0B",
};

function makeShadow() {
  return { type: "outer", blur: 8, offset: 3, angle: 135, color: "000000", opacity: 0.12 };
}

// ══════════════════════════════════════════════════════════════
// 슬라이드 1 — 타이틀
// ══════════════════════════════════════════════════════════════
{
  const s = pres.addSlide();
  s.background = { color: C.navy };

  // 왼쪽 포인트 바
  s.addShape(pres.shapes.RECTANGLE, { x: 0, y: 0, w: 0.15, h: 5.625, fill: { color: C.mint }, line: { color: C.mint } });

  // 배경 장식 원
  s.addShape(pres.shapes.OVAL, { x: 7.5, y: -0.8, w: 3.5, h: 3.5, fill: { color: C.blue, transparency: 70 }, line: { color: C.blue, transparency: 70 } });
  s.addShape(pres.shapes.OVAL, { x: 8.5, y: 2.8, w: 2.2, h: 2.2, fill: { color: C.teal, transparency: 75 }, line: { color: C.teal, transparency: 75 } });

  s.addText("API Gateway", {
    x: 0.5, y: 1.2, w: 9, h: 0.9,
    fontSize: 52, bold: true, color: C.white,
    fontFace: "Calibri", margin: 0,
  });
  s.addText("프로젝트 구현 현황 및 로드맵", {
    x: 0.5, y: 2.2, w: 9, h: 0.6,
    fontSize: 22, color: C.mint, fontFace: "Calibri", margin: 0,
  });

  // 구분선
  s.addShape(pres.shapes.RECTANGLE, { x: 0.5, y: 3.0, w: 4, h: 0.04, fill: { color: C.teal }, line: { color: C.teal } });

  s.addText("Spring Cloud Gateway  ·  JWT  ·  Redis  ·  PostgreSQL  ·  Prometheus", {
    x: 0.5, y: 3.2, w: 9, h: 0.4,
    fontSize: 13, color: C.gray, fontFace: "Calibri", margin: 0,
  });
  s.addText("2026. 06", {
    x: 0.5, y: 4.8, w: 9, h: 0.4,
    fontSize: 12, color: C.gray, fontFace: "Calibri", margin: 0,
  });
}

// ══════════════════════════════════════════════════════════════
// 슬라이드 2 — 왜 API Gateway인가
// ══════════════════════════════════════════════════════════════
{
  const s = pres.addSlide();
  s.background = { color: C.white };

  s.addText("왜 API Gateway인가?", {
    x: 0.5, y: 0.25, w: 9, h: 0.65,
    fontSize: 30, bold: true, color: C.navy, fontFace: "Calibri", margin: 0,
  });
  s.addShape(pres.shapes.RECTANGLE, { x: 0.5, y: 0.92, w: 9, h: 0.03, fill: { color: C.light }, line: { color: C.light } });

  // ── Before 박스
  s.addShape(pres.shapes.RECTANGLE, {
    x: 0.4, y: 1.1, w: 4.1, h: 3.9,
    fill: { color: "FEF2F2" }, line: { color: "FECACA", pt: 1.5 }, shadow: makeShadow(),
  });
  s.addText("Before  (Gateway 없음)", {
    x: 0.4, y: 1.15, w: 4.1, h: 0.45,
    fontSize: 14, bold: true, color: "DC2626", align: "center", fontFace: "Calibri", margin: 0,
  });

  const beforeItems = [
    "클라이언트가 8081, 8082 직접 호출",
    "인증 로직을 서버마다 각각 구현",
    "서버 추가 시 클라이언트 코드 수정",
    "장애 감지를 클라이언트가 직접 처리",
    "공통 로깅/모니터링 불가",
  ];
  beforeItems.forEach((txt, i) => {
    s.addShape(pres.shapes.OVAL, { x: 0.6, y: 1.75 + i * 0.58, w: 0.22, h: 0.22, fill: { color: "DC2626" }, line: { color: "DC2626" } });
    s.addText(txt, { x: 0.92, y: 1.73 + i * 0.58, w: 3.4, h: 0.3, fontSize: 12, color: C.dark, fontFace: "Calibri", margin: 0 });
  });

  // ── After 박스
  s.addShape(pres.shapes.RECTANGLE, {
    x: 5.5, y: 1.1, w: 4.1, h: 3.9,
    fill: { color: "F0FDF4" }, line: { color: "86EFAC", pt: 1.5 }, shadow: makeShadow(),
  });
  s.addText("After  (Gateway 도입)", {
    x: 5.5, y: 1.15, w: 4.1, h: 0.45,
    fontSize: 14, bold: true, color: "16A34A", align: "center", fontFace: "Calibri", margin: 0,
  });

  const afterItems = [
    "클라이언트는 8080 하나만 알면 됨",
    "인증/로깅/제한 게이트웨이에서 통합",
    "백엔드 추가 시 코드 변경 없음",
    "헬스체크로 장애 자동 감지·복구",
    "Prometheus로 통합 모니터링",
  ];
  afterItems.forEach((txt, i) => {
    s.addShape(pres.shapes.OVAL, { x: 5.7, y: 1.75 + i * 0.58, w: 0.22, h: 0.22, fill: { color: "16A34A" }, line: { color: "16A34A" } });
    s.addText(txt, { x: 6.02, y: 1.73 + i * 0.58, w: 3.4, h: 0.3, fontSize: 12, color: C.dark, fontFace: "Calibri", margin: 0 });
  });

  // 화살표
  s.addShape(pres.shapes.RECTANGLE, { x: 4.55, y: 2.95, w: 0.9, h: 0.08, fill: { color: C.teal }, line: { color: C.teal } });
  s.addText("▶", { x: 4.6, y: 2.75, w: 0.8, h: 0.5, fontSize: 22, color: C.teal, align: "center", margin: 0 });
}

// ══════════════════════════════════════════════════════════════
// 슬라이드 3 — 현재 구현 완료 기능
// ══════════════════════════════════════════════════════════════
{
  const s = pres.addSlide();
  s.background = { color: C.light };

  s.addText("현재 구현 완료된 기능", {
    x: 0.5, y: 0.2, w: 9, h: 0.6,
    fontSize: 30, bold: true, color: C.navy, fontFace: "Calibri", margin: 0,
  });
  s.addShape(pres.shapes.RECTANGLE, { x: 0.5, y: 0.82, w: 9, h: 0.03, fill: { color: "CBD5E1" }, line: { color: "CBD5E1" } });

  const features = [
    { icon: "🔐", title: "JWT 인증/인가", desc: "Bearer 토큰 검증\nUSER / ADMIN 권한 분리" },
    { icon: "🗝️", title: "API Key 관리", desc: "키 발급·삭제·조회\nDB(PostgreSQL) 영속 저장" },
    { icon: "🚦", title: "Rate Limiting", desc: "API Key별 분당 제한\nRedis 기반 분산 처리" },
    { icon: "⚖️", title: "로드밸런싱", desc: "backend-01, 02 분산\nSpring LoadBalancer" },
    { icon: "🏥", title: "헬스체크", desc: "5초 주기 자동 감지\n장애 서버 자동 제외·복구" },
    { icon: "📊", title: "로깅 & 모니터링", desc: "Trace ID 전파\nPrometheus + Grafana" },
  ];

  features.forEach((f, i) => {
    const col = i % 3;
    const row = Math.floor(i / 3);
    const x = 0.4 + col * 3.1;
    const y = 1.05 + row * 2.1;

    s.addShape(pres.shapes.RECTANGLE, {
      x, y, w: 2.9, h: 1.85,
      fill: { color: C.white }, line: { color: "E2E8F0", pt: 1 }, shadow: makeShadow(),
    });
    // 상단 색상 바
    s.addShape(pres.shapes.RECTANGLE, { x, y, w: 2.9, h: 0.22, fill: { color: C.teal }, line: { color: C.teal } });

    s.addText(f.icon + "  " + f.title, {
      x: x + 0.12, y: y + 0.3, w: 2.65, h: 0.4,
      fontSize: 13, bold: true, color: C.navy, fontFace: "Calibri", margin: 0,
    });
    s.addText(f.desc, {
      x: x + 0.12, y: y + 0.75, w: 2.65, h: 0.85,
      fontSize: 11, color: C.gray, fontFace: "Calibri", margin: 0,
    });
  });
}

// ══════════════════════════════════════════════════════════════
// 슬라이드 4 — 요청 처리 파이프라인
// ══════════════════════════════════════════════════════════════
{
  const s = pres.addSlide();
  s.background = { color: C.white };

  s.addText("요청 처리 파이프라인 (필터 실행 순서)", {
    x: 0.5, y: 0.2, w: 9, h: 0.6,
    fontSize: 30, bold: true, color: C.navy, fontFace: "Calibri", margin: 0,
  });
  s.addShape(pres.shapes.RECTANGLE, { x: 0.5, y: 0.82, w: 9, h: 0.03, fill: { color: "CBD5E1" }, line: { color: "CBD5E1" } });

  const steps = [
    { label: "Client",        color: "94A3B8", order: "" },
    { label: "Logging\n(-100)", color: C.teal,  order: "1" },
    { label: "Admin Filter\n(-10)", color: "7C3AED", order: "2" },
    { label: "API Key\n(-3)",  color: C.blue,   order: "3" },
    { label: "Rate Limit\n(-2)", color: "D97706", order: "4" },
    { label: "JWT Auth\n(-1)", color: "DC2626",  order: "5" },
    { label: "Backend\n01/02", color: C.done,   order: "" },
  ];

  const boxW = 1.18, boxH = 0.95, startX = 0.25, y = 1.6;
  const gap = (10 - startX * 2 - boxW * steps.length) / (steps.length - 1);

  steps.forEach((st, i) => {
    const x = startX + i * (boxW + gap);
    s.addShape(pres.shapes.RECTANGLE, {
      x, y, w: boxW, h: boxH,
      fill: { color: st.color }, line: { color: st.color }, shadow: makeShadow(),
    });
    s.addText(st.label, {
      x, y: y + 0.08, w: boxW, h: boxH - 0.08,
      fontSize: 10, bold: true, color: C.white,
      align: "center", valign: "middle", fontFace: "Calibri", margin: 0,
    });
    if (i < steps.length - 1) {
      s.addText("▶", {
        x: x + boxW + gap * 0.15, y: y + 0.3, w: gap * 0.7, h: 0.4,
        fontSize: 14, color: "CBD5E1", align: "center", margin: 0,
      });
    }
  });

  // 설명 카드 3개
  const notes = [
    { x: 0.5,  title: "Admin 경로", desc: "/gateway/admin/**\n→ X-ADMIN-KEY 헤더 검증\n→ 통과 시 JWT·API Key 검사 생략" },
    { x: 3.6,  title: "일반 API 경로", desc: "/api/**\n→ API Key 검증 후 Redis Rate Limit\n→ JWT 검증 후 백엔드 라우팅" },
    { x: 6.7,  title: "인증 제외 경로", desc: "/auth/**\n→ JWT·API Key 검사 없이 통과\n→ 토큰 발급 전용 엔드포인트" },
  ];
  notes.forEach(n => {
    s.addShape(pres.shapes.RECTANGLE, {
      x: n.x, y: 3.05, w: 2.9, h: 2.1,
      fill: { color: C.light }, line: { color: "CBD5E1", pt: 1 }, shadow: makeShadow(),
    });
    s.addText(n.title, {
      x: n.x + 0.15, y: 3.12, w: 2.6, h: 0.38,
      fontSize: 12, bold: true, color: C.navy, fontFace: "Calibri", margin: 0,
    });
    s.addText(n.desc, {
      x: n.x + 0.15, y: 3.55, w: 2.6, h: 1.45,
      fontSize: 10.5, color: C.gray, fontFace: "Calibri", margin: 0,
    });
  });
}

// ══════════════════════════════════════════════════════════════
// 슬라이드 5 — 다음 단계: Eureka 도입
// ══════════════════════════════════════════════════════════════
{
  const s = pres.addSlide();
  s.background = { color: C.white };

  s.addText("다음 단계: Eureka Service Discovery 도입", {
    x: 0.5, y: 0.2, w: 9, h: 0.6,
    fontSize: 26, bold: true, color: C.navy, fontFace: "Calibri", margin: 0,
  });
  s.addShape(pres.shapes.RECTANGLE, { x: 0.5, y: 0.82, w: 9, h: 0.03, fill: { color: "CBD5E1" }, line: { color: "CBD5E1" } });

  // 현재
  s.addShape(pres.shapes.RECTANGLE, {
    x: 0.4, y: 1.05, w: 4.2, h: 4.1,
    fill: { color: "FFF7ED" }, line: { color: "FED7AA", pt: 1.5 }, shadow: makeShadow(),
  });
  s.addText("현재 구조", {
    x: 0.4, y: 1.08, w: 4.2, h: 0.42,
    fontSize: 14, bold: true, color: "C2410C", align: "center", fontFace: "Calibri", margin: 0,
  });
  const curItems = [
    "백엔드 주소 코드에 하드코딩",
    "(localhost:8081, localhost:8082)",
    "",
    "헬스체크 직접 구현",
    "(BackendHealthChecker.java)",
    "",
    "서버 추가 시 코드 수정 필요",
    "확장성 낮음",
  ];
  curItems.forEach((t, i) => {
    if (t) {
      s.addText((t.startsWith("(") ? "    " : "• ") + t, {
        x: 0.6, y: 1.62 + i * 0.38, w: 3.8, h: 0.35,
        fontSize: 11, color: t.startsWith("(") ? C.gray : C.dark, fontFace: "Calibri",
        italic: t.startsWith("("), margin: 0,
      });
    }
  });

  // 화살표
  s.addText("⟹", { x: 4.65, y: 2.8, w: 0.7, h: 0.6, fontSize: 26, color: C.mint, align: "center", margin: 0 });

  // 도입 후
  s.addShape(pres.shapes.RECTANGLE, {
    x: 5.4, y: 1.05, w: 4.2, h: 4.1,
    fill: { color: "F0FDF4" }, line: { color: "86EFAC", pt: 1.5 }, shadow: makeShadow(),
  });
  s.addText("Eureka 도입 후", {
    x: 5.4, y: 1.08, w: 4.2, h: 0.42,
    fontSize: 14, bold: true, color: "16A34A", align: "center", fontFace: "Calibri", margin: 0,
  });
  const newItems = [
    "Eureka Server 중앙 등록소 운영",
    "",
    "백엔드가 시작 시 자동 등록",
    "Gateway가 목록 자동 조회",
    "",
    "서버 추가 = 설정 한 줄만 추가",
    "헬스체크 Eureka가 담당",
    "기존 코드 대폭 제거 가능",
  ];
  newItems.forEach((t, i) => {
    if (t) {
      s.addText("• " + t, {
        x: 5.6, y: 1.62 + i * 0.38, w: 3.8, h: 0.35,
        fontSize: 11, color: C.dark, fontFace: "Calibri", margin: 0,
      });
    }
  });

  // 제거될 코드 안내
  s.addShape(pres.shapes.RECTANGLE, {
    x: 0.4, y: 5.05, w: 9.2, h: 0.35,
    fill: { color: "FEF9C3" }, line: { color: "FDE047", pt: 1 },
  });
  s.addText("제거 예정: ServerRegistry.java  ·  BackendHealthChecker.java  ·  HealthCheckScheduler.java  ·  LoadBalancerConfig.java", {
    x: 0.5, y: 5.08, w: 9, h: 0.28,
    fontSize: 10, color: "92400E", align: "center", fontFace: "Calibri", margin: 0,
  });
}

// ══════════════════════════════════════════════════════════════
// 슬라이드 6 — 전체 로드맵
// ══════════════════════════════════════════════════════════════
{
  const s = pres.addSlide();
  s.background = { color: C.navy };

  s.addText("전체 로드맵", {
    x: 0.5, y: 0.2, w: 9, h: 0.65,
    fontSize: 32, bold: true, color: C.white, fontFace: "Calibri", margin: 0,
  });
  s.addShape(pres.shapes.RECTANGLE, { x: 0.5, y: 0.88, w: 9, h: 0.03, fill: { color: C.teal }, line: { color: C.teal } });

  const phases = [
    {
      phase: "Phase 1  완료",
      color: C.done,
      items: [
        "Spring Cloud Gateway 기반 구성",
        "JWT 인증 / ADMIN 권한 분리",
        "API Key 발급·관리 (PostgreSQL)",
        "Redis Rate Limiting",
        "헬스체크 + 로드밸런싱",
        "Prometheus / Grafana 연동",
      ],
    },
    {
      phase: "Phase 2  진행 예정",
      color: C.todo,
      items: [
        "Eureka Server 모듈 추가",
        "Gateway → Eureka Client 전환",
        "backend-01/02 → Eureka 자동 등록",
        "기존 수동 헬스체크 코드 제거",
        "전체 Docker 컨테이너화",
      ],
    },
    {
      phase: "Phase 3  향후 검토",
      color: "94A3B8",
      items: [
        "Circuit Breaker (연속 장애 즉시 차단)",
        "Refresh Token 도입",
        "응답 캐싱 (Redis)",
        "IP 차단 / 화이트리스트",
        "환경별 설정 분리 (dev / prod)",
      ],
    },
  ];

  phases.forEach((p, i) => {
    const x = 0.35 + i * 3.15;
    s.addShape(pres.shapes.RECTANGLE, {
      x, y: 1.1, w: 2.95, h: 4.15,
      fill: { color: "0D1B2A" }, line: { color: p.color, pt: 2 }, shadow: makeShadow(),
    });
    s.addShape(pres.shapes.RECTANGLE, { x, y: 1.1, w: 2.95, h: 0.42, fill: { color: p.color }, line: { color: p.color } });
    s.addText(p.phase, {
      x: x + 0.1, y: 1.13, w: 2.75, h: 0.36,
      fontSize: 12, bold: true, color: C.white, align: "center", fontFace: "Calibri", margin: 0,
    });
    p.items.forEach((item, j) => {
      s.addShape(pres.shapes.OVAL, { x: x + 0.18, y: 1.68 + j * 0.56, w: 0.16, h: 0.16, fill: { color: p.color }, line: { color: p.color } });
      s.addText(item, {
        x: x + 0.42, y: 1.65 + j * 0.56, w: 2.4, h: 0.32,
        fontSize: 10.5, color: C.white, fontFace: "Calibri", margin: 0,
      });
    });
  });
}

// ══════════════════════════════════════════════════════════════
// 슬라이드 7 — 마무리
// ══════════════════════════════════════════════════════════════
{
  const s = pres.addSlide();
  s.background = { color: C.navy };

  s.addShape(pres.shapes.RECTANGLE, { x: 0, y: 0, w: 0.15, h: 5.625, fill: { color: C.mint }, line: { color: C.mint } });
  s.addShape(pres.shapes.OVAL, { x: 7.5, y: -0.8, w: 3.5, h: 3.5, fill: { color: C.blue, transparency: 70 }, line: { color: C.blue, transparency: 70 } });

  s.addText("감사합니다", {
    x: 0.5, y: 1.6, w: 9, h: 1.0,
    fontSize: 52, bold: true, color: C.white, align: "center", fontFace: "Calibri", margin: 0,
  });
  s.addText("API Gateway 프로젝트", {
    x: 0.5, y: 2.8, w: 9, h: 0.5,
    fontSize: 18, color: C.mint, align: "center", fontFace: "Calibri", margin: 0,
  });
  s.addShape(pres.shapes.RECTANGLE, { x: 3.5, y: 3.45, w: 3, h: 0.04, fill: { color: C.teal }, line: { color: C.teal } });
  s.addText("Spring Cloud Gateway  ·  Eureka  ·  Redis  ·  PostgreSQL", {
    x: 0.5, y: 3.65, w: 9, h: 0.4,
    fontSize: 12, color: C.gray, align: "center", fontFace: "Calibri", margin: 0,
  });
}

// ── 저장
pres.writeFile({ fileName: "C:/dev/02_work/ApiGatewayTest/API_Gateway_발표자료.pptx" })
  .then(() => console.log("✅ 저장 완료: API_Gateway_발표자료.pptx"))
  .catch(e => console.error("❌ 에러:", e));
