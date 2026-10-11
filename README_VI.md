<div align="center">

# 💎 CappyShards

### **Hệ thống Kinh tế Shard Tinh thể, Thưởng AFK & Cửa hàng GUI Thông minh cho PaperMC**

*Siêu nhẹ • Tương thích 100% Minecraft Client Vanilla • Cơ chế Chống Dupe Nguyên tử • SQLite Asynchronous & WAL Mode*

---

[![PaperMC](https://img.shields.io/badge/Paper-1.21+-F44336?style=for-the-badge&logo=papermc&logoColor=white)](https://papermc.io/)
[![Java](https://img.shields.io/badge/Java-21-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://adoptium.net/)
[![SQLite](https://img.shields.io/badge/SQLite-HikariCP_WAL-003B57?style=for-the-badge&logo=sqlite&logoColor=white)](https://www.sqlite.org/)
[![PlaceholderAPI](https://img.shields.io/badge/PlaceholderAPI-H%E1%BB%97_tr%E1%BB%A3-4CAF50?style=for-the-badge)](https://wiki.placeholderapi.com/)
[![bStats](https://img.shields.io/badge/bStats-34192-008CBA?style=for-the-badge)](https://bstats.org/plugin/bukkit/CappyShards/34192)
[![Modrinth](https://img.shields.io/badge/Modrinth-Kh%E1%BA%A3_d%E1%BB%A5ng-00AF5C?style=for-the-badge&logo=modrinth&logoColor=white)](https://modrinth.com/)

[**Tính năng**](#-tính-năng-nổi-bật) • [**Cài đặt Nhanh**](#-hướng-dẫn-cài-đặt-nhanh) • [**Cửa hàng Shop**](#-5-cửa-hàng-gui-thông-minh-100-vanilla) • [**Bình Multiplier**](#-vật-phẩm-tăng-tốc-shard-multiplier) • [**Placeholders**](#-tích-hợp-placeholderapi-papi) • [**Lệnh & Quyền**](#-lệnh--phân-quyền) • [**Cấu hình**](#-các-tệp-cấu-hình)

<br>

🌐 **Ngôn ngữ:** [English](README.md) • [Tiếng Việt](README_VI.md)

</div>

<br>

---

## 🌟 Tại sao nên chọn CappyShards?

Bạn đã quá mệt mỏi với các plugin kinh tế cồng kềnh gây tụt TPS, ngốn RAM, rò rỉ bộ nhớ hoặc bị khai thác lỗi click chuột để dupe vật phẩm?

**CappyShards** là giải pháp máy chủ hiện đại, chuẩn production được xây dựng từ nền tảng dành riêng cho các cụm máy chủ Paper/Purpur 1.21+. Plugin mang đến loại tiền tệ cao cấp phụ (**Shards/Mảnh tinh thể**), khu vực nhận thưởng AFK với Action Bar ưu tiên tuyệt đối không bị đè bởi plugin khác, hệ thống cửa hàng GUI trực quan tùy chỉnh số lượng theo Đơn vị và Stack ngay lập tức, vật phẩm kích hoạt hệ số nhân dùng ngay, và hệ thống chống dupe bằng cờ khóa nguyên tử đa luồng.

**Đặc biệt nhất? Không cần cài đặt bất kỳ bản mod client hay resource pack nào.** Mọi tính năng đều hiển thị mượt mà trên Minecraft nguyên bản (Vanilla Client)!

---

## 🚀 Tính năng Nổi bật

### ⚡ 1. Kinh tế Shard Hiệu năng Cao & Chuẩn TPS 20
- **Giao dịch Cơ sở dữ liệu Bất đồng bộ (Async):** Vận hành trên SQLite kết hợp **HikariCP** và chế độ **WAL Mode (`Write-Ahead Logging`)**. Không gây giật khung hình hay nghẽn Main Thread của server.
- **Khóa Chống Dupe Nguyên tử (`AtomicBoolean`):** Khóa giao dịch độc quyền cấp mili-giây, ngăn chặn hoàn toàn việc click chuột siêu tốc, bug kéo thả hòm đồ hay lỗi gửi gói tin giả mạo.
- **Chuyển tiền An toàn giữa Người chơi:** Lệnh `/shard pay` tích hợp thuật toán kiểm tra số dư, chống chuyển cho chính mình và khóa giao dịch tránh race condition.

### ⏳ 2. Thưởng Shard AFK & Action Bar Ưu tiên Tuyệt đối
- **Cấu hình Sẵn sàng:** Thiết lập mặc định cho thế giới `ShardAFK`.
- **Đếm thời gian Miễn nhiễm Lag TPS:** Đo lường trực tiếp theo mốc thời gian thực (`System.currentTimeMillis()`), do đó dù server có lag đột xuất thì bộ đếm giờ nhận thưởng của người chơi vẫn hoàn toàn chính xác.
- **Engine Action Bar Ưu tiên Cao:** Nhịp phát xung bất đồng bộ mỗi **0.5s (10 ticks)**, đảm bảo thanh đếm ngược luôn sáng rõ và không bị các plugin chat/actionbar khác ghi đè mất.
- **Banner Chúc mừng Nổi bật:** Hiển thị thông báo nhận thưởng bắt mắt trong 3 giây trước khi trở lại đồng hồ đếm ngược.
- **Tỉ lệ thưởng theo Rank:** Dễ dàng cấu hình số Shard nhận được cho từng bậc xếp hạng (VIP+, VIP, Default).

### 🛡️ 3. Tích hợp Bảo mật Máy chủ
- **Chống Tạo Quá nhiều Tài khoản (Anti-Alt):** Giới hạn số lượng tài khoản tối đa được phép online đồng thời trên cùng một địa chỉ IP.
- **Lá chắn Chống VPN & Proxy (Anti-VPN):** Kiểm tra bất đồng bộ trước khi người chơi đăng nhập qua API tình báo mạng (như `proxycheck.io`), lưu cache trong RAM để từ chối bot/VPN ngay từ cửa vào mà không làm chậm server.

### 🧪 4. Bình Tăng tốc Shard Multiplier (Instant-Use)
- **Kích hoạt Tức thì qua Chuột phải:** Nước khoáng tăng tốc giúp nhân đôi/nhân ba lượng Shard nhận được khi AFK.
- **Không Cần Chờ Uống:** Uống được ngay lập tức mà không phải tốn 1.6 giây animation uống potion như mặc định.
- **Hiệu ứng Âm thanh & Hạt lung linh:** Phát âm thanh level up và hiệu ứng hạt rực rỡ khi sử dụng.
- **Tùy biến 100% trong `item.yml`:** Tự do chỉnh sửa tên, lore, custom model data, âm thanh và hạt hiệu ứng.

### 🛒 5. Cửa hàng GUI Thông minh (100% Vanilla)
Trải nghiệm mua sắm tiện nghi, thân thiện mà không cần Texture Pack hay ItemsAdder:
- **Duyệt Danh mục Rõ ràng:** Phân loại vật phẩm theo từng bộ phận (Vũ khí, Tiêu hao, Nguyên liệu...) với biểu tượng và tiêu đề gradient bắt mắt.
- **Mua theo Đơn vị (1-64) hoặc Mua theo Stack (1-36):** Người chơi linh hoạt chuyển đổi giữa chế độ mua lẻ từng món hoặc mua cả rương/stack thông qua các nút bấm `+` và `-` tiện lợi.
- **Nhận diện Vật phẩm Không xếp chồng:** Tự động vô hiệu hóa chế độ mua theo stack đối với các vật phẩm không stack được (kiếm, giáp, công cụ) để tránh lỗi.
- **Xem trước Hóa đơn Minh bạch:** Tóm tắt chính xác số lượng mua, tổng số Shard cần trả và số dư hiện có trước khi xác nhận.
- **Hỗ trợ Phần thưởng Lệnh (Console Commands):** Cho phép gắn các lệnh như phát Crate Key, cấp Rank hoặc phát vật phẩm custom từ các plugin khác trong `shop.yml`.
- **Kiểm tra Chỗ trống Hòm đồ:** Tự động tính toán diện tích túi đồ của người chơi, ngăn chặn việc mua hàng khi hòm đồ đầy làm rơi đồ ra đất.

### 💤 6. Tích lũy Shard khi Offline (Offline Shard Accumulation)
- **Nhận Thưởng Thụ động cả khi Rời mạng:** Người chơi vẫn tiếp tục tích lũy Shard kể cả khi không online trên server!
- **Giới hạn Thời gian theo Rank:** Cấu hình thời gian tích lũy tối đa cho từng rank trong `config.yml` (ví dụ: rank VIP+ tối đa 12 tiếng, VIP tối đa 6 tiếng, Default không có).
- **Phân quyền Linh hoạt:** Kiểm soát thông qua quyền gốc `cappy.offline.shard` hoặc quyền theo từng rank cụ thể (`cappy.offline.shard.<rank>`).
- **Thông báo Chào mừng Trở lại:** Khi người chơi đăng nhập lại, hệ thống sẽ gửi tin nhắn chi tiết thông báo số Shard đã tích lũy kèm thời gian đã offline.

---

## ⚡ Vật phẩm Tăng tốc Shard Multiplier

Quản trị viên có thể phát các bình Shard Multiplier làm phần thưởng nhiệm vụ, quà mở rương crate hoặc bán trên webstore:

```bash
# Phát cho bản thân bình nhân x2 Shard trong 30 phút:
/cappy give shard_multiplier 2 30

# Phát cho người chơi Steve bình nhân x1.5 Shard trong 60 phút:
/cappy give Steve shard_multiplier 1.5 60
```

- **Nhấp chuột phải dùng ngay:** Dùng ở bất kỳ đâu trong game để lập tức kích hoạt bộ đếm thời gian.
- **Nhân đôi phần thưởng:** Trong suốt thời gian hiệu lực, mọi khoản thưởng Shard AFK sẽ được nhân theo hệ số (ví dụ: 10 Shards × 2.0 = 20 Shards).
- **Hiển thị trên Action Bar:** Thanh Action Bar tự động bổ sung ký hiệu hệ số nhân (ví dụ: `Nhận Shards sau: 45s (x2)`).

---

## 🧩 Tích hợp PlaceholderAPI (PAPI)

CappyShards cung cấp đầy đủ các placeholder sẵn sàng đưa vào Scoreboard, Danh sách TAB, Khung chat, Hologram hoặc DeluxeMenus:

| Placeholder | Mô tả | Đầu ra Mẫu |
| :--- | :--- | :--- |
| `%cappyshards_balance%` | Số dư Shard chính xác (số nguyên thô) | `1500` |
| `%cappyshards_shards%` | Tên gọi khác của `%cappyshards_balance%` | `1500` |
| `%cappyshards_balance_formatted%` | Số dư Shard có dấu phẩy phân tách hàng nghìn | `1,500` |
| `%cappyshards_shards_formatted%` | Tên gọi khác của số dư có dấu phẩy | `1,500` |
| `%cappyshards_afk_time%` | Số giây còn lại cho đến lần nhận thưởng AFK kế tiếp | `42` |
| `%cappyshards_afk_remaining%` | Tên gọi khác của thời gian đếm ngược AFK | `42` |
| `%cappyshards_rank_reward%` | Số Shard nhận được mỗi chu kỳ theo rank của người chơi | `50` |
| `%cappyshards_multiplier%` | Hệ số nhân đang kích hoạt (hoặc `1` nếu không có) | `2` |
| `%cappyshards_multiplayer%` | Tên gọi khác của `%cappyshards_multiplier%` | `2` |
| `%cappyshards_multiplier_formatted%` | Hệ số nhân có tiền tố `x` | `x2` |
| `%cappyshards_multiplier_remaining%` | Thời gian hiệu lực còn lại của bình booster (tính bằng giây) | `1799` |
| `%cappyshards_multiplier_time_formatted%` | Thời gian còn lại hiển thị dạng phút giây thân thiện | `29m 59s` |
| `%cappyshards_has_multiplier%` | Kiểm tra người chơi có đang dùng bình nhân không | `true` / `false` |
| `%cappyshards_multiplier_tag%` | Thẻ định dạng booster (rỗng nếu không dùng) | ` (x2)` |
| `%cappyshards_offline_max_hours%` | Số giờ tối đa người chơi được phép tích lũy Shard khi offline | `6` |

---

## 🎮 Lệnh & Phân quyền

### Danh sách Lệnh
| Lệnh | Viết tắt | Quyền hạn | Mô tả |
| :--- | :--- | :--- | :--- |
| `/cappyshard` | `/shard`, `/shards`, `/cshard`, `/cappy` | `cappyshards.use` | Xem số dư Shard của bản thân |
| `/cappyshard pay <người_chơi> <số_lượng>` | | `cappyshards.use.pay` | Chuyển Shard cho một người chơi đang online |
| `/cappyshard shop` | `/shardshop` | `cappyshards.use.shop` | Mở menu Cửa hàng Shard |
| `/cappyshard give [người_chơi] shard_multiplier <hệ_số> [số_phút]` | | `cappyshards.admin` | Phát bình thuốc Shard Multiplier dùng ngay |
| `/cappyshard add <người_chơi> <số_lượng>` | | `cappyshards.admin` | Cộng thêm Shard vào số dư của người chơi |
| `/cappyshard set <người_chơi> <số_lượng>` | | `cappyshards.admin` | Đặt số dư Shard chính xác cho người chơi |
| `/cappyshard remove <người_chơi> <số_lượng>` | | `cappyshards.admin` | Khấu trừ Shard từ tài khoản của người chơi |
| `/cappyshard reload` | | `cappyshards.admin` | Tải lại toàn bộ tệp cấu hình, vật phẩm và khởi động lại manager |

### Danh sách Quyền hạn (Permissions)
- `cappyshards.use` *(Mặc định: true)* — Cho phép kiểm tra số dư Shard.
- `cappyshards.use.pay` *(Mặc định: true)* — Cho phép chuyển Shard cho người khác.
- `cappyshards.use.shop` *(Mặc định: true)* — Cho phép mở menu Cửa hàng Shard.
- `cappyshards.admin` *(Mặc định: op)* — Quyền quản trị viên (set, add, remove, give, reload).
- `cappyshards.bypass.zone` *(Mặc định: op)* — Nhận thưởng AFK ở bất kỳ thế giới nào.
- `cappyshards.rank.<tên_rank>` — Áp dụng mức thưởng Shard tương ứng theo cấu hình trong `config.yml`.
- `cappy.offline.shard` — Quyền cơ bản để tích lũy Shard khi offline.
- `cappy.offline.shard.<tên_rank>` — Quyền tích lũy offline theo từng rank cụ thể (`cappy.offline.shard.vipplus`, `cappy.offline.shard.vip`).

---

## 🗂️ Các Tệp Cấu hình

Tất cả các tệp đều sử dụng định dạng màu sắc hiện đại **MiniMessage** (hỗ trợ chuyển màu Gradient và mã Hex):

- **`config.yml`**: Thiết lập chung, chu kỳ AFK, thế giới kích hoạt, Anti-Alt, Anti-VPN và các mức Rank (thưởng online + thời gian tích lũy offline tối đa).
- **`messages.yml`**: Tùy biến 100% các câu thông báo, cảnh báo lỗi và tiền tố prefix trong chat.
- **`item.yml`**: Tùy chỉnh bình nước Shard Multiplier (tên, lore, custom model data, âm thanh và hiệu ứng hạt).
- **`Shop/catalogue.yml`**: Cấu hình các danh mục trong shop, số ô kích thước, biểu tượng và lore.
- **`Shop/shop.yml`**: Cấu hình chi tiết từng món đồ bán trong mỗi danh mục, giá tiền, số lượng nhận được và các lệnh console đi kèm.

---

## 📥 Hướng dẫn Cài đặt Nhanh

1. Tải về bản build mới nhất `CappyShards-1.0.0.jar` từ mục [Releases](https://github.com/quocdat251107-del/CappyShard/releases) hoặc [Modrinth](https://modrinth.com/).
2. Đặt file `.jar` vào thư mục `plugins/` của máy chủ.
3. *(Tùy chọn)* Cài đặt thêm [PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/) để tích hợp bảng điểm Scoreboard & TAB.
4. Khởi động máy chủ. Các tệp cấu hình mẫu sẽ được tự động tạo.
5. Chỉnh sửa `config.yml`, `messages.yml`, `item.yml` và `Shop/shop.yml` theo nhu cầu server của bạn.
6. Sử dụng lệnh `/cappyshard reload` để cập nhật thay đổi ngay trong game mà không cần khởi động lại server!

---

## 🛠️ Build từ Mã Nguồn

```bash
git clone https://github.com/quocdat251107-del/CappyShard.git
cd CappyShard
./gradlew build
```
File jar hoàn chỉnh sau khi build sẽ nằm tại thư mục `build/libs/CappyShards-1.0.0.jar`.

---

<div align="center">

**CappyShards** là dự án mã nguồn mở được phát triển với ❤️ bởi **Cappy Community**.  
Nếu bạn yêu thích plugin này, hãy dành tặng 1 ⭐ sao trên kho lưu trữ GitHub nhé!

</div>
