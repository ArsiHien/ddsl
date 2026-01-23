# 3.4 Phân tích phụ thuộc và liên thông trong mô hình DDSL

Tài liệu này mô tả cách mô hình hóa đồ thị phụ thuộc giữa Aggregate và Module, thuật toán phát hiện chu trình dựa trên DFS tô màu (3-color DFS) nhằm đảm bảo nguyên lý Acyclic Dependency Principle (ADP), và cơ chế phân tích tính liên thông (reachability) để kiểm tra ranh giới giữa các Bounded Context.

---

## 3.4.1. Mô hình hóa sự phụ thuộc

### Mục tiêu
Biểu diễn các mối phụ thuộc (dependency) giữa:
- Các Aggregate bên trong (và giữa) các Module
- Các Module bên trong (và giữa) các Bounded Context

### Mô hình đồ thị
- Đồ thị có hướng G = (V, E)
- Tập đỉnh V gồm hai loại node:
  - Aggregate: đại diện cho một Aggregate Root hoặc một Aggregate trong domain model
  - Module: đại diện cho một module/khoanh vùng kỹ thuật trong cùng Bounded Context
- Cạnh (u → v) ∈ E mang ý nghĩa “u phụ thuộc vào v” (u imports/uses/references v)

### Thuộc tính node
- id: định danh duy nhất; khuyến nghị dạng Context.Module.Aggregate hoặc Context.Module
- kind: "Aggregate" | "Module"
- context: tên Bounded Context chứa node

Ví dụ:
- Sales.Ordering.Order (Aggregate)
- Sales.Billing (Module)

### Quy tắc sinh cạnh
Tạo cạnh có hướng u → v khi phát hiện một trong các quan hệ sau trong mô hình/metadata:
- Aggregate u tham chiếu type/ID của Aggregate v
- Repository/Service của Module u phụ thuộc vào Entity/VO/Service ở Module v
- Phụ thuộc compile-time (import), phụ thuộc runtime (gọi phương thức trực tiếp)

Lưu ý về ngữ nghĩa ranh giới:
- Nếu context(u) ≠ context(v), cạnh trực tiếp u → v thường là vi phạm (xem 3.4.3); thay vào đó nên dùng sự kiện domain (publish/subscribe), ACL hoặc anti-corruption layer

### Biểu diễn dữ liệu (gợi ý)
- Dùng danh sách kề (adjacency list) cho G để phục vụ các thuật toán O(V+E)
- Ví dụ cấu trúc JSON cho một node:
```
{
  "id": "Sales.Ordering.Order",
  "kind": "Aggregate",
  "context": "Sales"
}
```

---

## 3.4.2. Thuật toán phát hiện chu trình (Cycle Detection)

### Mục tiêu
Phát hiện xem đồ thị phụ thuộc có chu trình hay không nhằm đảm bảo nguyên lý ADP (Acyclic Dependency Principle): đồ thị phụ thuộc giữa module/aggregate phải không có chu trình.

### Thuật toán DFS tô màu (3-color DFS)
- WHITE (trắng): đỉnh chưa thăm
- GRAY (xám): đỉnh đang ở trên ngăn xếp đệ quy (đang thăm)
- BLACK (đen): đỉnh đã thăm xong
- Khi duyệt cạnh u → v:
  - Nếu color[v] = GRAY, phát hiện back-edge ⇒ có chu trình
  - Nếu color[v] = WHITE, tiếp tục đệ quy

#### Pseudo-code
```
enum Color { WHITE, GRAY, BLACK }

boolean hasCycle(Graph G) {
  Map<Node, Color> color = new HashMap<>();
  Map<Node, Node> parent = new HashMap<>();

  for (Node u : G.vertices()) {
    color.put(u, WHITE);
    parent.put(u, null);
  }

  for (Node u : G.vertices()) {
    if (color.get(u) == WHITE) {
      List<Node> cycle = dfsVisit(G, u, color, parent);
      if (!cycle.isEmpty()) {
        // có chu trình, có thể trả về cycle để báo cáo
        return true;
      }
    }
  }
  return false; // không có chu trình
}

List<Node> dfsVisit(Graph G, Node u, Map<Node, Color> color, Map<Node, Node> parent) {
  color.put(u, GRAY);
  for (Node v : G.adj(u)) {
    if (color.get(v) == GRAY) {
      return buildCyclePath(u, v, parent); // phát hiện back-edge u→v
    }
    if (color.get(v) == WHITE) {
      parent.put(v, u);
      List<Node> found = dfsVisit(G, v, color, parent);
      if (!found.isEmpty()) return found;
    }
  }
  color.put(u, BLACK);
  return Collections.emptyList();
}

List<Node> buildCyclePath(Node u, Node v, Map<Node, Node> parent) {
  // tái tạo chu trình từ v (đã GRAY) về lại v qua chuỗi parent bắt đầu ở u
  List<Node> path = new ArrayList<>();
  path.add(v);
  Node x = u;
  while (x != null && x != v) {
    path.add(x);
    x = parent.get(x);
  }
  if (x == v) path.add(v);
  Collections.reverse(path);
  return path; // v → ... → u → v
}
```

### Cơ sở toán học (phác thảo)
- Với đồ thị có hướng, duyệt DFS tạo thành rừng DFS. Một cạnh u → v là back-edge khi v là tổ tiên của u trong cây DFS ⇒ tồn tại chu trình.
- Mệnh đề: G có chu trình ⇔ tồn tại back-edge trong DFS của G.
  - (⇒) Nếu có chu trình C: x₀ → x₁ → … → xₖ → x₀, khi DFS đi vào x₀ và lần lượt thăm theo cạnh trên C, tới lúc gặp cạnh về đỉnh tổ tiên sẽ tạo back-edge.
  - (⇐) Nếu tồn tại back-edge u → v với v là tổ tiên của u, thì đường đi v ↣ u trong cây DFS kết hợp cạnh u → v tạo chu trình.
- Hệ quả: Không có back-edge ⇔ đồ thị là DAG ⇔ thỏa ADP.
- Độ phức tạp: O(V + E) thời gian, O(V) không gian.

### Ứng dụng vào ADP
- Chạy 3-color DFS trên đồ thị phụ thuộc Aggregate/Module.
- Nếu phát hiện chu trình, báo lỗi ADP, kèm đường đi chu trình để refactor (ví dụ tách module, đảo chiều phụ thuộc, dùng sự kiện, áp dụng DIP).

---

## 3.4.3. Phân tích tính liên thông (Reachability Analysis)

### Mục tiêu
Kiểm tra ranh giới giữa các Bounded Context: phát hiện các đường đi (path) hoặc phụ thuộc trực tiếp không hợp lệ giữa các context khác nhau.

### Định nghĩa
- reach(u, v) = true nếu tồn tại đường đi có hướng từ u đến v trong G.
- Với u, v thuộc các context khác nhau, cần ràng buộc kiểu phụ thuộc (event, ACL, RPC thông qua anti-corruption layer) thay vì tham chiếu trực tiếp.

### Thuật toán kiểm tra reachability
Có thể dùng BFS hoặc DFS từ một nguồn u để tìm tập Reach(u) = { v | reach(u, v) }.

#### Pseudo-code (BFS)
```
Set<Node> reachable(Graph G, Node s) {
  Set<Node> visited = new HashSet<>();
  Queue<Node> q = new ArrayDeque<>();
  visited.add(s);
  q.add(s);
  while (!q.isEmpty()) {
    Node u = q.remove();
    for (Node v : G.adj(u)) {
      if (!visited.contains(v)) {
        visited.add(v);
        q.add(v);
      }
    }
  }
  return visited;
}
```

### Kiểm tra ranh giới Bounded Context
- Quy tắc cơ bản:
  - Nếu context(u) ≠ context(v) và tồn tại cạnh trực tiếp u → v kiểu “hard reference” (import/call trực tiếp), coi là vi phạm boundary.
  - Cho phép u → v nếu edge.type ∈ { "event-publish", "event-subscribe", "acl" } và có lớp adapter/bộ chuyển đổi rõ ràng.
- Kiểm tra:
  - Duyệt từng cạnh (u → v): nếu context(u) ≠ context(v) và edge.type không thuộc danh sách được phép, báo vi phạm.
  - Tuỳ chính sách, cũng có thể kiểm tra reachability đa bước: nếu tồn tại đường đi u ↣ v băng qua nhiều context với toàn các cạnh “hard reference”, báo cảnh báo nâng cao (rủi ro kết dính liên-context).

#### Pseudo-code kiểm tra vi phạm cross-context
```
List<Violation> checkCrossContext(Graph G) {
  List<Violation> out = new ArrayList<>();
  for (Edge e : G.edges()) {
    Node u = e.from();
    Node v = e.to();
    if (!u.context().equals(v.context())) {
      if (!isAllowedCrossEdge(e.type())) {
        out.add(new Violation(u, v, e.type(), "Direct cross-context dependency is not allowed"));
      }
    }
  }
  return out;
}

boolean isAllowedCrossEdge(EdgeType t) {
  return t == EVENT_PUBLISH || t == EVENT_SUBSCRIBE || t == ACL;
}
```

### Kết quả mong đợi
- Danh sách các vi phạm ranh giới với đường đi ngắn nhất minh họa, gợi ý refactor: dùng domain events, thêm anti-corruption layer, hoặc di chuyển trách nhiệm giữa các module/context.

---

## Liên hệ với mã nguồn dự án
- Phát hiện chu trình/ân dụng DFS tô màu: xem `src/main/java/uet/ndh/ddsl/core/semantic/DependencyAnalyzer.java` (thuật toán O(V+E)).
- Phân tích/kiểm tra boundary có thể tích hợp ở lớp Semantic Analyzer/Validator: `TwoPassSemanticAnalyzer`, `DDDTacticalValidator`.

## Gợi ý báo cáo và hiển thị
- Với chu trình: xuất đường đi cụ thể (node1 → node2 → … → node1).
- Với vi phạm cross-context: ghi rõ `u.context != v.context`, loại cạnh và khuyến nghị thay thế (event/ACL).
- Có thể sinh báo cáo HTML/Markdown để tích hợp vào báo cáo build.

