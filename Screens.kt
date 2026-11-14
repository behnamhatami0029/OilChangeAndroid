package com.oilchange.app

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Process
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import java.io.File

// ---------------- خانه ----------------
@Composable
fun HomeScreen(dao: AppDao, nav: NavController) {
    val ctx = LocalContext.current
    val cc by dao.customerCount().collectAsState(0)
    val vc by dao.vehicleCount().collectAsState(0)
    val ms = remember { Jalali.monthStart() }
    val sc by remember { dao.serviceCountSince(ms) }.collectAsState(0)
    val rev by remember { dao.revenueSince(ms) }.collectAsState(0L)
    val shop = prefs(ctx).getString("shop", "") ?: ""
    Column(Modifier.padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("🛢️ " + shop.ifBlank { "مدیریت تعویض روغنی" }, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { Stat("مشتریان", fa(cc), Modifier.weight(1f)); Stat("وسایل نقلیه", fa(vc), Modifier.weight(1f)) }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { Stat("سرویس این ماه", fa(sc), Modifier.weight(1f)); Stat("درآمد این ماه (تومان)", money(rev), Modifier.weight(1f)) }
        Button({ nav.navigate("customers") }, Modifier.fillMaxWidth()) { Text("+ ثبت مشتری / سرویس جدید") }
        OutlinedButton({ nav.navigate("vehicles") }, Modifier.fillMaxWidth()) { Text("جستجوی خودرو با پلاک") }
    }
}

// ---------------- مشتریان ----------------
@Composable
fun CustomersScreen(dao: AppDao, nav: NavController) {
    val scope = rememberCoroutineScope()
    var q by remember { mutableStateOf("") }
    var add by remember { mutableStateOf(false) }
    val list by remember(q) { dao.customers(en(q)) }.collectAsState(emptyList())
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.padding(12.dp)) {
            Field("جستجوی نام یا موبایل", q, { q = it })
            Spacer(Modifier.height(8.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp), contentPadding = PaddingValues(bottom = 80.dp)) {
                items(list, key = { it.id }) { c ->
                    Card(Modifier.fillMaxWidth().clickable { nav.navigate("customer/${c.id}") }) {
                        Column(Modifier.padding(12.dp)) { Text(c.name, fontWeight = FontWeight.Bold); if (c.mobile.isNotBlank()) Text(fa(c.mobile), style = MaterialTheme.typography.bodySmall) }
                    }
                }
            }
        }
        ExtendedFloatingActionButton({ add = true }, Modifier.align(Alignment.BottomStart).padding(16.dp)) { Text("+ مشتری جدید") }
    }
    if (add) {
        var n by remember { mutableStateOf("") }; var m by remember { mutableStateOf("") }; var note by remember { mutableStateOf("") }
        AlertDialog(onDismissRequest = { add = false }, title = { Text("مشتری جدید") },
            confirmButton = { TextButton({ if (n.isNotBlank()) { scope.launch { val id = dao.insert(Customer(name = n.trim(), mobile = en(m.trim()), note = note.trim())); add = false; nav.navigate("customer/$id") } } }) { Text("ذخیره") } },
            dismissButton = { TextButton({ add = false }) { Text("انصراف") } },
            text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { Field("نام و نام خانوادگی", n, { n = it }); Field("موبایل", m, { m = it }, number = true); Field("توضیحات", note, { note = it }) } })
    }
}

@Composable
fun CustomerScreen(dao: AppDao, nav: NavController, id: Long) {
    val scope = rememberCoroutineScope()
    val c by remember(id) { dao.customer(id) }.collectAsState(null)
    val vs by remember(id) { dao.vehiclesOf(id) }.collectAsState(emptyList())
    var addV by remember { mutableStateOf(false) }
    var edit by remember { mutableStateOf(false) }
    var del by remember { mutableStateOf(false) }
    val cu = c ?: return
    Column(Modifier.padding(12.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(cu.name, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        if (cu.mobile.isNotBlank()) Text("موبایل: " + fa(cu.mobile))
        if (cu.note.isNotBlank()) Text(cu.note)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton({ edit = true }) { Text("ویرایش") }
            OutlinedButton({ del = true }) { Text("حذف مشتری") }
        }
        Divider()
        Text("وسایل نقلیه (${fa(vs.size)})", fontWeight = FontWeight.Bold)
        vs.forEach { v ->
            Card(Modifier.fillMaxWidth().clickable { nav.navigate("vehicle/${v.id}") }) {
                Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    PlateBadge(v.plateText); Text(v.brandModel)
                }
            }
        }
        Button({ addV = true }, Modifier.fillMaxWidth()) { Text("+ افزودن وسیله نقلیه") }
    }
    if (addV) VehicleDialog(null, { addV = false }) { v -> scope.launch { val vid = dao.insert(v.copy(customerId = id)); addV = false; nav.navigate("vehicle/$vid") } }
    if (edit) {
        var n by remember { mutableStateOf(cu.name) }; var m by remember { mutableStateOf(cu.mobile) }; var note by remember { mutableStateOf(cu.note) }
        AlertDialog(onDismissRequest = { edit = false }, title = { Text("ویرایش مشتری") },
            confirmButton = { TextButton({ if (n.isNotBlank()) { scope.launch { dao.update(cu.copy(name = n.trim(), mobile = en(m.trim()), note = note.trim())); edit = false } } }) { Text("ذخیره") } },
            dismissButton = { TextButton({ edit = false }) { Text("انصراف") } },
            text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { Field("نام", n, { n = it }); Field("موبایل", m, { m = it }, number = true); Field("توضیحات", note, { note = it }) } })
    }
    if (del) AlertDialog(onDismissRequest = { del = false }, title = { Text("حذف مشتری") }, text = { Text("مشتری با تمام خودروها و سرویس‌هایش حذف شود؟") },
        confirmButton = { TextButton({ scope.launch { dao.delete(cu); del = false; nav.popBackStack() } }) { Text("حذف") } },
        dismissButton = { TextButton({ del = false }) { Text("انصراف") } })
}

// ---------------- خودروها ----------------
@Composable
fun VehiclesScreen(dao: AppDao, nav: NavController) {
    var q by remember { mutableStateOf("") }
    val list by remember(q) { dao.vehicles(en(q), searchDigits(q)) }.collectAsState(emptyList())
    Column(Modifier.padding(12.dp)) {
        Field("جستجو با پلاک، مدل، نام یا موبایل", q, { q = it })
        Spacer(Modifier.height(8.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(list, key = { it.v.id }) { r ->
                Card(Modifier.fillMaxWidth().clickable { nav.navigate("vehicle/${r.v.id}") }) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) { PlateBadge(r.v.plateText); Text(r.v.brandModel) }
                        Text("${r.customerName} | آخرین سرویس: ${Jalali.fmt(r.lastAt, false)} | بعدی: ${r.nextKm?.takeIf { it > 0 }?.let { fa(it) } ?: "—"}", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@Composable
fun VehicleScreen(dao: AppDao, nav: NavController, id: Long) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val v by remember(id) { dao.vehicle(id) }.collectAsState(null)
    val cust by remember(v?.customerId) { dao.customer(v?.customerId ?: 0L) }.collectAsState(null)
    val last by remember(id) { dao.lastService(id) }.collectAsState(null)
    val list by remember(id) { dao.services(id) }.collectAsState(emptyList())
    var cur by remember { mutableStateOf("") }
    var qr by remember { mutableStateOf(false) }
    var edit by remember { mutableStateOf(false) }
    var del by remember { mutableStateOf(false) }
    val vv = v ?: return
    val txt = statusText(ctx, vv, last)
    LazyColumn(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) { PlateBadge(vv.plateText); Text("${vv.brandModel} ${vv.color} ${fa(vv.year)}") }
                Text("مشتری: ${cust?.name ?: ""}")
                Card { Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (last == null) Text("هنوز سرویسی ثبت نشده است.") else {
                        Text("آخرین سرویس: ${Jalali.fmt(last!!.serviceAt)}")
                        Text("کیلومتر هنگام سرویس: ${fa(last!!.odometer)}")
                        Text("روغن: ${last!!.oilName.ifBlank { "—" }}")
                        Text("سرویس بعدی در کیلومتر: ${fa(last!!.nextKm)}", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                        Field("کیلومتر فعلی خودرو", cur, { cur = it }, number = true)
                        val c = toInt(cur)
                        if (c > 0 && last!!.nextKm > 0) { val r = last!!.nextKm - c; Text(if (r > 0) "${fa(r)} کیلومتر تا سرویس بعدی" else "موعد تعویض روغن گذشته است!", fontWeight = FontWeight.Bold) }
                    }
                } }
                Button({ nav.navigate("service/$id") }, Modifier.fillMaxWidth()) { Text("+ ثبت سرویس جدید") }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton({ qr = true }, Modifier.weight(1f)) { Text("QR") }
                    OutlinedButton({
                        val m = cust?.mobile ?: ""
                        val i = if (m.isNotBlank()) Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$m")).putExtra("sms_body", txt)
                        else Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, txt), null)
                        ctx.startActivity(i)
                    }, Modifier.weight(1f)) { Text("پیام") }
                    OutlinedButton({ edit = true }, Modifier.weight(1f)) { Text("ویرایش") }
                    OutlinedButton({ del = true }, Modifier.weight(1f)) { Text("حذف") }
                }
                Text("تاریخچه سرویس‌ها", fontWeight = FontWeight.Bold)
                if (list.isEmpty()) Text("سرویسی ثبت نشده است.")
            }
        }
        items(list, key = { it.id }) { s ->
            Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("📅 ${Jalali.fmt(s.serviceAt)}", fontWeight = FontWeight.Bold)
                Text("کیلومتر: ${fa(s.odometer)} | سرویس بعدی: ${if (s.nextKm > 0) fa(s.nextKm) else "—"}")
                if (s.oilName.isNotBlank()) Text("روغن: ${s.oilName}")
                if (s.items.isNotBlank()) Text("✔ " + s.items.split("|").joinToString("، "))
                if (s.price > 0) Text("مبلغ: ${money(s.price)} تومان", style = MaterialTheme.typography.bodySmall)
                if (s.note.isNotBlank()) Text("📝 ${s.note}", style = MaterialTheme.typography.bodySmall)
                TextButton({ scope.launch { dao.delete(s) } }) { Text("حذف سرویس") }
            } }
        }
    }
    if (qr) AlertDialog(onDismissRequest = { qr = false }, confirmButton = { TextButton({ qr = false }) { Text("بستن") } },
        title = { Text("QR وضعیت سرویس") },
        text = { Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(remember(txt) { qrBitmap(txt).asImageBitmap() }, null, Modifier.size(260.dp))
            Text("با اسکن این QR، تاریخ آخرین سرویس و کیلومتر بعدی بدون اینترنت نمایش داده می‌شود.", style = MaterialTheme.typography.bodySmall)
        } })
    if (edit) VehicleDialog(vv, { edit = false }) { nv -> scope.launch { dao.update(nv); edit = false } }
    if (del) AlertDialog(onDismissRequest = { del = false }, title = { Text("حذف وسیله نقلیه") }, text = { Text("این خودرو و تمام سرویس‌هایش حذف شود؟") },
        confirmButton = { TextButton({ scope.launch { dao.delete(vv); del = false; nav.popBackStack() } }) { Text("حذف") } },
        dismissButton = { TextButton({ del = false }) { Text("انصراف") } })
}

// ---------------- ثبت سرویس ----------------
@Composable
fun ServiceFormScreen(dao: AppDao, nav: NavController, vid: Long) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val v by remember(vid) { dao.vehicle(vid) }.collectAsState(null)
    val types by dao.types().collectAsState(emptyList())
    val oils by dao.oils().collectAsState(emptyList())
    val checked = remember { mutableStateListOf<Long>() }
    var extra by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(Jalali.nowInput()) }
    var odo by remember { mutableStateOf("") }
    var oilName by remember { mutableStateOf("") }
    var interval by remember { mutableStateOf("") }
    var next by remember { mutableStateOf("") }
    var nextEdited by remember { mutableStateOf(false) }
    var price by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    LaunchedEffect(odo, interval) {
        if (!nextEdited) { val o = toInt(odo); val i = toInt(interval); next = if (o > 0 && i > 0) (o + i).toString() else "" }
    }
    Column(Modifier.padding(12.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("ثبت سرویس ${v?.plateText ?: ""}", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text("کارهای انجام‌شده", fontWeight = FontWeight.Bold)
        types.forEach { t ->
            Row(Modifier.fillMaxWidth().clickable { if (t.id in checked) checked.remove(t.id) else checked.add(t.id) }, verticalAlignment = Alignment.CenterVertically) {
                Checkbox(t.id in checked, { if (it) checked.add(t.id) else checked.remove(t.id) }); Text(t.title)
            }
        }
        Field("سایر کارها (با ویرگول جدا کنید)", extra, { extra = it })
        Field("تاریخ و ساعت شمسی (مثال 1405/08/23 14:30)", date, { date = it })
        Field("کیلومتر فعلی خودرو", odo, { odo = it }, number = true)
        Dropdown("انتخاب روغن از لیست", oils.map { "${it.name} (${fa(it.intervalKm)} km)" }, "") { i -> oilName = oils[i].name; interval = oils[i].intervalKm.toString(); nextEdited = false }
        Field("نام روغن", oilName, { oilName = it })
        Field("مناسب برای چند کیلومتر", interval, { interval = it; nextEdited = false }, number = true)
        Field("کیلومتر سرویس بعدی", next, { next = it; nextEdited = true }, number = true)
        Field("مبلغ (تومان) - اختیاری", price, { price = it }, number = true)
        Field("توضیحات", note, { note = it })
        Button({
            val at = Jalali.parse(date)
            val o = toInt(odo)
            if (at == null) { toast(ctx, "تاریخ شمسی نامعتبر است"); return@Button }
            if (o <= 0) { toast(ctx, "کیلومتر را وارد کنید"); return@Button }
            val items = types.filter { it.id in checked }.map { it.title } + extra.replace(",", "،").split("،").map { it.trim() }.filter { it.isNotEmpty() }
            val iv = toInt(interval); val nx = if (toInt(next) > 0) toInt(next) else if (iv > 0) o + iv else 0
            scope.launch {
                dao.insert(Service(vehicleId = vid, serviceAt = at, odometer = o, oilName = oilName.trim(), intervalKm = iv, nextKm = nx, price = toInt(price).toLong(), note = note.trim(), items = items.joinToString("|")))
                toast(ctx, "سرویس ثبت شد"); nav.popBackStack()
            }
        }, Modifier.fillMaxWidth()) { Text("ثبت سرویس") }
    }
}

// ---------------- گزارش‌ها ----------------
@Composable
fun ReportsScreen(dao: AppDao, nav: NavController) {
    val ctx = LocalContext.current
    val days = prefs(ctx).getInt("days", 120)
    val from = remember { System.currentTimeMillis() - 365L * 86400000L }
    val rows by remember { dao.servicesSince(from) }.collectAsState(emptyList())
    val veh by remember { dao.vehicles("", "") }.collectAsState(emptyList())
    val limit = System.currentTimeMillis() - days * 86400000L
    val od = veh.filter { (it.lastAt ?: Long.MAX_VALUE) < limit }.sortedBy { it.lastAt }
    val months = rows.groupBy { Jalali.monthKey(it.s.serviceAt) }.toSortedMap(compareByDescending { it.first })
    val itemsCount = rows.flatMap { it.s.items.split("|").filter { x -> x.isNotBlank() } }.groupingBy { it }.eachCount().entries.sortedByDescending { it.value }.take(8)
    val oilCount = rows.filter { it.s.oilName.isNotBlank() }.groupingBy { it.s.oilName }.eachCount().entries.sortedByDescending { it.value }.take(6)
    val custCount = rows.groupingBy { it.customerName }.eachCount().entries.sortedByDescending { it.value }.take(6)
    LazyColumn(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item { Text("گزارش ۱۲ ماه اخیر", fontSize = 20.sp, fontWeight = FontWeight.Bold) }
        item { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Stat("تعداد سرویس", fa(rows.size), Modifier.weight(1f)); Stat("درآمد (تومان)", money(rows.sumOf { it.s.price }), Modifier.weight(1f)) } }
        item { OutlinedButton({
            val sb = StringBuilder("\uFEFFتاریخ,پلاک,مشتری,کیلومتر,روغن,کیلومتر بعدی,مبلغ,کارها\n")
            fun q(s: String) = "\"" + s.replace("\"", "\"\"") + "\""
            rows.forEach { sb.append(listOf(q(Jalali.fmt(it.s.serviceAt)), q(it.plateText), q(it.customerName), it.s.odometer, q(it.s.oilName), it.s.nextKm, it.s.price, q(it.s.items.replace("|", "، "))).joinToString(",")).append("\n") }
            val f = File(ctx.cacheDir, "services.csv"); f.writeText(sb.toString()); shareFile(ctx, f, "text/csv")
        }, Modifier.fillMaxWidth()) { Text("خروجی اکسل (CSV)") } }
        item { Text("آمار ماهانه", fontWeight = FontWeight.Bold) }
        items(months.entries.toList()) { e -> Card(Modifier.fillMaxWidth()) { Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(e.key.second); Text("${fa(e.value.size)} سرویس | ${money(e.value.sumOf { it.s.price })}") } } }
        item { Text("پرتکرارترین کارها", fontWeight = FontWeight.Bold) }
        items(itemsCount) { Text("• ${it.key}: ${fa(it.value)}") }
        item { Text("روغن‌های پرمصرف", fontWeight = FontWeight.Bold) }
        items(oilCount) { Text("• ${it.key}: ${fa(it.value)}") }
        item { Text("مشتریان پرمراجعه", fontWeight = FontWeight.Bold) }
        items(custCount) { Text("• ${it.key}: ${fa(it.value)}") }
        item { Text("نیازمند مراجعه (بیش از ${fa(days)} روز از آخرین سرویس)", fontWeight = FontWeight.Bold) }
        items(od) { r -> Card(Modifier.fillMaxWidth().clickable { nav.navigate("vehicle/${r.v.id}") }) { Column(Modifier.padding(12.dp)) {
            Text("${r.v.plateText} - ${r.customerName}"); Text("آخرین سرویس: ${Jalali.fmt(r.lastAt, false)}", style = MaterialTheme.typography.bodySmall) } } }
        if (od.isEmpty()) item { Text("موردی نیست") }
    }
}

// ---------------- تنظیمات ----------------
@Composable
fun SettingsScreen(dao: AppDao) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val oils by dao.oils().collectAsState(emptyList())
    val types by dao.types().collectAsState(emptyList())
    var shop by remember { mutableStateOf(prefs(ctx).getString("shop", "") ?: "") }
    var days by remember { mutableStateOf(prefs(ctx).getInt("days", 120).toString()) }
    var on by remember { mutableStateOf("") }; var oi by remember { mutableStateOf("") }; var tt by remember { mutableStateOf("") }
    val pick = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            AppDb.close()
            val f = ctx.getDatabasePath(AppDb.NAME)
            File(f.path + "-wal").delete(); File(f.path + "-shm").delete()
            ctx.contentResolver.openInputStream(uri)?.use { i -> f.outputStream().use { o -> i.copyTo(o) } }
            toast(ctx, "بازیابی انجام شد. برنامه را دوباره باز کنید.")
            (ctx as Activity).finishAffinity(); Process.killProcess(Process.myPid())
        }
    }
    LazyColumn(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item { Text("تنظیمات", fontSize = 20.sp, fontWeight = FontWeight.Bold) }
        item { Field("نام تعویض روغنی (در پیام‌ها و QR)", shop, { shop = it }) }
        item { Field("یادآوری مراجعه بعد از چند روز", days, { days = it }, number = true) }
        item { Button({ prefs(ctx).edit().putString("shop", shop.trim()).putInt("days", maxOf(1, toInt(days))).apply(); toast(ctx, "ذخیره شد") }, Modifier.fillMaxWidth()) { Text("ذخیره") } }
        item { Divider() }
        item { Text("روغن‌ها و بازه تعویض", fontWeight = FontWeight.Bold) }
        item { Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            Field("نام روغن", on, { on = it }, Modifier.weight(2f)); Field("کیلومتر", oi, { oi = it }, Modifier.weight(1.3f), true)
            Button({ if (on.isNotBlank() && toInt(oi) > 0) scope.launch { dao.insert(Oil(name = on.trim(), intervalKm = toInt(oi))); on = ""; oi = "" } }) { Text("+") } } }
        items(oils, key = { "o${it.id}" }) { o -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("${o.name} - ${fa(o.intervalKm)} km"); TextButton({ scope.launch { dao.delete(o) } }) { Text("حذف") } } }
        item { Divider() }
        item { Text("کارهای سرویس (تیک‌ها)", fontWeight = FontWeight.Bold) }
        item { Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            Field("مورد جدید", tt, { tt = it }, Modifier.weight(1f)); Button({ if (tt.isNotBlank()) scope.launch { dao.insert(ServiceType(title = tt.trim())); tt = "" } }) { Text("+") } } }
        items(types, key = { "t${it.id}" }) { t -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(t.title); TextButton({ scope.launch { dao.delete(t) } }) { Text("حذف") } } }
        item { Divider() }
        item { Text("پشتیبان‌گیری از دیتابیس", fontWeight = FontWeight.Bold) }
        item { Text("اطلاعات فقط روی همین گوشی ذخیره می‌شود. حتماً مرتب پشتیبان بگیرید.", style = MaterialTheme.typography.bodySmall) }
        item { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button({
                AppDb.get(ctx).openHelper.writableDatabase.query("PRAGMA wal_checkpoint(FULL)").close()
                val out = File(ctx.cacheDir, "oilchange-backup.db"); ctx.getDatabasePath(AppDb.NAME).copyTo(out, true); shareFile(ctx, out, "application/octet-stream")
            }, Modifier.weight(1f)) { Text("گرفتن پشتیبان") }
            OutlinedButton({ pick.launch(arrayOf("*/*")) }, Modifier.weight(1f)) { Text("بازیابی") } } }
    }
}
