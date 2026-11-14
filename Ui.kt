package com.oilchange.app

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import java.io.File

val LETTERS = listOf("الف","ب","پ","ت","ث","ج","چ","د","ذ","ر","ز","ژ","س","ش","ص","ض","ط","ظ","ع","غ","ف","ق","ک","گ","ل","م","ن","و","ه","ی")
val VT = linkedMapOf("car" to "سواری", "motorcycle" to "موتورسیکلت", "truck" to "وانت / کامیون", "other" to "سایر")

fun toast(c: Context, s: String) = Toast.makeText(c, s, Toast.LENGTH_SHORT).show()
fun prefs(c: Context) = c.getSharedPreferences("p", 0)
fun searchDigits(q: String): String = if (q.isBlank()) "" else en(q).filter { it.isDigit() }.ifEmpty { "@@@" }

fun qrBitmap(text: String, size: Int = 600): Bitmap {
    val m = QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, size, size, mapOf<EncodeHintType, Any>(EncodeHintType.CHARACTER_SET to "UTF-8", EncodeHintType.MARGIN to 1))
    val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565)
    for (x in 0 until size) for (y in 0 until size) bmp.setPixel(x, y, if (m[x, y]) 0xFF000000.toInt() else 0xFFFFFFFF.toInt())
    return bmp
}

fun shareFile(ctx: Context, f: File, mime: String) {
    val u = FileProvider.getUriForFile(ctx, ctx.packageName + ".fp", f)
    ctx.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
        type = mime; putExtra(Intent.EXTRA_STREAM, u); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }, null))
}

fun statusText(ctx: Context, v: Vehicle, l: Service?): String = buildString {
    val shop = prefs(ctx).getString("shop", "") ?: ""
    if (shop.isNotBlank()) appendLine(shop)
    appendLine("پلاک: ${v.plateText}")
    if (l == null) append("هنوز سرویسی ثبت نشده است.") else {
        appendLine("آخرین سرویس: ${Jalali.fmt(l.serviceAt)}")
        appendLine("کیلومتر: ${fa(l.odometer)}")
        if (l.oilName.isNotBlank()) appendLine("روغن: ${l.oilName}")
        append("سرویس بعدی در کیلومتر: ${fa(l.nextKm)}")
    }
}

@Composable
fun Field(label: String, value: String, onChange: (String) -> Unit, modifier: Modifier = Modifier.fillMaxWidth(), number: Boolean = false) {
    OutlinedTextField(value, onChange, label = { Text(label) }, modifier = modifier, singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = if (number) KeyboardType.Number else KeyboardType.Text))
}

@Composable
fun Dropdown(label: String, options: List<String>, selected: String, modifier: Modifier = Modifier.fillMaxWidth(), onSel: (Int) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box(modifier) {
        OutlinedButton({ open = true }, Modifier.fillMaxWidth()) { Text(if (selected.isBlank()) label else selected) }
        DropdownMenu(open, { open = false }) {
            options.forEachIndexed { i, o -> DropdownMenuItem(text = { Text(o) }, onClick = { open = false; onSel(i) }) }
        }
    }
}

@Composable
fun Stat(title: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier) { Column(Modifier.padding(12.dp)) { Text(title, style = MaterialTheme.typography.bodySmall); Text(value, style = MaterialTheme.typography.headlineSmall) } }
}

@Composable
fun PlateBadge(text: String) {
    Surface(border = androidx.compose.foundation.BorderStroke(2.dp, androidx.compose.ui.graphics.Color.Black), shape = MaterialTheme.shapes.small) {
        Text(text, Modifier.padding(horizontal = 10.dp, vertical = 2.dp), style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
fun VehicleDialog(init: Vehicle?, onDismiss: () -> Unit, onSave: (Vehicle) -> Unit) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    var type by remember { mutableStateOf(init?.type ?: "car") }
    var brand by remember { mutableStateOf(init?.brandModel ?: "") }
    var color by remember { mutableStateOf(init?.color ?: "") }
    var year by remember { mutableStateOf(init?.year ?: "") }
    val pp = init?.plateKey?.split("-") ?: emptyList()
    val car = pp.size == 4
    var p1 by remember { mutableStateOf(if (car) pp[0] else "") }
    var pl by remember { mutableStateOf(if (car) pp[1] else LETTERS[0]) }
    var p2 by remember { mutableStateOf(if (car) pp[2] else "") }
    var pc by remember { mutableStateOf(if (car) pp[3] else "") }
    var pt by remember { mutableStateOf(if (!car) init?.plateKey ?: "" else "") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (init == null) "افزودن وسیله نقلیه" else "ویرایش وسیله نقلیه") },
        confirmButton = {
            TextButton({
                val key: String; val txt: String
                if (type == "motorcycle") {
                    val d = en(pt).filter { it.isDigit() }
                    if (d.length < 5) { toast(ctx, "شماره پلاک موتور را صحیح وارد کنید"); return@TextButton }
                    key = d; txt = fa(d)
                } else {
                    val a = en(p1).filter { it.isDigit() }; val b = en(p2).filter { it.isDigit() }; val c = en(pc).filter { it.isDigit() }
                    if (a.length != 2 || b.length != 3 || c.length != 2) { toast(ctx, "پلاک را کامل وارد کنید (۲ رقم، حرف، ۳ رقم، کد ۲ رقمی)"); return@TextButton }
                    key = "$a-$pl-$b-$c"; txt = fa("$a $pl $b ایران $c")
                }
                if (brand.isBlank()) { toast(ctx, "مدل خودرو را وارد کنید"); return@TextButton }
                onSave((init ?: Vehicle(customerId = 0, type = type, brandModel = "", plateKey = "", plateText = ""))
                    .copy(type = type, brandModel = brand.trim(), color = color.trim(), year = year.trim(), plateKey = key, plateText = txt))
            }) { Text("ذخیره") }
        },
        dismissButton = { TextButton(onDismiss) { Text("انصراف") } },
        text = {
            Column(Modifier.verticalScroll(androidx.compose.foundation.rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Dropdown("نوع", VT.values.toList(), VT[type] ?: "") { type = VT.keys.toList()[it] }
                Field("مدل / برند", brand, { brand = it })
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Field("رنگ", color, { color = it }, Modifier.weight(1f))
                    Field("سال ساخت", year, { year = it }, Modifier.weight(1f), number = true)
                }
                if (type == "motorcycle") Field("شماره پلاک (فقط عدد)", pt, { pt = it }, number = true)
                else Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Field("۲ رقم", p1, { p1 = it }, Modifier.weight(1f), true)
                    Dropdown("حرف", LETTERS, pl, Modifier.weight(1.1f)) { pl = LETTERS[it] }
                    Field("۳ رقم", p2, { p2 = it }, Modifier.weight(1.3f), true)
                    Text("ایران")
                    Field("کد", pc, { pc = it }, Modifier.weight(1f), true)
                }
            }
        }
    )
}
