## Cách Đóng Góp (Làm Việc Nhóm Hiệu Quả)

### 1. **Chiến Lược Branching**

Để tránh xung đột mã nguồn, các thành viên nên tuân thủ chiến lược **branching**:

- **Nhánh Chính (`main`)**: Luôn chứa mã nguồn ổn định và sẵn sàng cho sản xuất. Không commit trực tiếp vào nhánh này.
- **Nhánh Tính Năng**: Mỗi thành viên tạo một nhánh riêng khi làm việc trên một task cụ thể. Ví dụ:
  - `feature/flight-booking`
  - `feature/admin-dashboard`

#### Các bước tạo và làm việc trên nhánh tính năng:

1. Lấy các thay đổi mới nhất từ nhánh `main`:

   ```bash
   git checkout main
   git pull origin main
   ```

2. Tạo một nhánh tính năng mới:

   ```bash
   git checkout -b feature/my-feature
   ```

3. Sau khi hoàn thành, commit và đẩy thay đổi lên:

   ```bash
   git add .
   git commit -m "feat: implement feature X"
   git push origin feature/my-feature
   ```

4. **Ai đó** review và merge/ tạo pull request.

### 2. **Quy Tắc Đặt Tên Commit**

Sử dụng các thông báo commit rõ ràng và nhất quán:

- **feat:** cho tính năng mới
- **fix:** cho sửa lỗi
- **refactor:** cho cấu trúc lại mã
- **chore:** cho công việc liên quan đến build tools hoặc dependencies
- **docs:** cho các thay đổi về tài liệu

Ví dụ:

```bash
feat: Thêm form đặt vé máy bay với validation
fix: Sửa lỗi chuyển đổi múi giờ cho giờ khởi hành
```

### 3. **Quy Trình Code Review**

- Mỗi pull request cần được review trước khi merge vào `main`.
