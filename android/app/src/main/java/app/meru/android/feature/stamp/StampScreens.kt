package app.meru.android.feature.stamp

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.meru.android.core.datastore.SessionStore
import app.meru.android.core.designsystem.components.MeruPrimaryButton
import app.meru.android.core.designsystem.components.MeruSecondaryButton
import app.meru.android.core.designsystem.theme.MeruAmber
import app.meru.android.core.designsystem.theme.MeruCyan
import app.meru.android.core.designsystem.theme.MeruElevated
import app.meru.android.core.designsystem.theme.MeruMuted
import app.meru.android.core.designsystem.theme.MeruTeal
import app.meru.android.core.designsystem.theme.MeruText
import app.meru.android.core.designsystem.theme.MeruVoid
import app.meru.android.core.network.ConfirmInvoiceResponse
import app.meru.android.core.network.ExtraDecisionRequest
import app.meru.android.core.network.InvoiceDto
import app.meru.android.core.network.JobDto
import app.meru.android.core.network.MeruApi
import app.meru.android.core.network.ReviewRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@HiltViewModel
class StampViewModel @Inject constructor(
    private val sessionStore: SessionStore,
    private val api: MeruApi,
) : ViewModel() {
    private val _jobs = MutableStateFlow<List<JobDto>>(emptyList())
    val jobs: StateFlow<List<JobDto>> = _jobs.asStateFlow()

    private val _invoices = MutableStateFlow<List<InvoiceDto>>(emptyList())
    val invoices: StateFlow<List<InvoiceDto>> = _invoices.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun refresh() {
        viewModelScope.launch {
            val token = sessionStore.session.first().accessToken ?: return@launch
            runCatching {
                _jobs.value = api.myJobs("Bearer $token").items
                _invoices.value = api.invoices("Bearer $token").items
                _error.value = null
            }.onFailure {
                _error.value = "Stamp offline"
            }
        }
    }

    fun decideExtra(jobId: String, extraId: String, approve: Boolean) {
        viewModelScope.launch {
            val token = sessionStore.session.first().accessToken ?: return@launch
            runCatching {
                api.decideExtra("Bearer $token", jobId, extraId, ExtraDecisionRequest(approve))
                refresh()
            }
        }
    }

    fun review(jobId: String, rating: Int) {
        viewModelScope.launch {
            val token = sessionStore.session.first().accessToken ?: return@launch
            runCatching {
                api.submitReview("Bearer $token", ReviewRequest(jobId, rating))
                refresh()
            }.onFailure {
                _error.value = it.message?.take(60) ?: "Review failed"
            }
        }
    }
}

@HiltViewModel
class InvoiceDetailViewModel @Inject constructor(
    private val sessionStore: SessionStore,
    private val api: MeruApi,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val invoiceId: String = checkNotNull(savedStateHandle["invoiceId"])

    private val _invoice = MutableStateFlow<InvoiceDto?>(null)
    val invoice: StateFlow<InvoiceDto?> = _invoice.asStateFlow()

    private val _confirm = MutableStateFlow<ConfirmInvoiceResponse?>(null)
    val confirm: StateFlow<ConfirmInvoiceResponse?> = _confirm.asStateFlow()

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun load() {
        viewModelScope.launch {
            val token = sessionStore.session.first().accessToken ?: return@launch
            runCatching {
                _invoice.value = api.invoice("Bearer $token", invoiceId)
            }.onFailure { _error.value = "Invoice offline" }
        }
    }

    fun confirmInvoice(onStamped: (ConfirmInvoiceResponse) -> Unit) {
        viewModelScope.launch {
            val token = sessionStore.session.first().accessToken ?: return@launch
            _busy.value = true
            runCatching {
                val res = api.confirmInvoice("Bearer $token", invoiceId)
                _confirm.value = res
                _invoice.value = res.invoice
                onStamped(res)
            }.onFailure {
                _error.value = it.message?.take(80) ?: "Confirm failed"
            }
            _busy.value = false
        }
    }

    fun dispute() {
        viewModelScope.launch {
            val token = sessionStore.session.first().accessToken ?: return@launch
            runCatching {
                _invoice.value = api.disputeInvoice("Bearer $token", invoiceId)
            }.onFailure { _error.value = "Dispute failed" }
        }
    }
}

@Composable
fun JobsInvoicesScreen(
    onBack: () -> Unit,
    onOpenInvoice: (String) -> Unit,
    driving: Boolean = false,
    viewModel: StampViewModel = hiltViewModel(),
) {
    val jobs by viewModel.jobs.collectAsState()
    val invoices by viewModel.invoices.collectAsState()
    val error by viewModel.error.collectAsState()
    LaunchedEffect(Unit) { viewModel.refresh() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MeruVoid)
            .padding(20.dp),
    ) {
        Text(
            "← Back",
            color = MeruTeal,
            modifier = Modifier
                .clickable(onClick = onBack)
                .padding(bottom = 12.dp),
        )
        Text("Stamp", color = MeruText, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Text("Jobs, invoices & vault writeback.", color = MeruMuted)
        if (driving) {
            Spacer(modifier = Modifier.height(8.dp))
            Text("Driving Mode — confirm locked", color = MeruAmber, fontSize = 13.sp)
        }
        error?.let {
            Spacer(modifier = Modifier.height(8.dp))
            Text(it, color = MeruAmber, fontSize = 13.sp)
        }
        Spacer(modifier = Modifier.height(16.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                Text("Open jobs", color = MeruText, fontWeight = FontWeight.SemiBold)
            }
            items(jobs, key = { it.id }) { job ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MeruElevated)
                        .padding(14.dp),
                ) {
                    Text(job.workshopName ?: job.workshopId, color = MeruText, fontWeight = FontWeight.SemiBold)
                    Text(job.status, color = MeruCyan, fontSize = 13.sp)
                    job.extras.filter { it.status == "pending" }.forEach { extra ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(extra.description, color = MeruText, fontSize = 13.sp)
                        Text("Est. PKR ${extra.estimatedCost.toInt()}", color = MeruMuted, fontSize = 12.sp)
                        if (!driving) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                MeruPrimaryButton(
                                    text = "Approve",
                                    onClick = { viewModel.decideExtra(job.id, extra.id, true) },
                                )
                                MeruSecondaryButton(
                                    text = "Deny",
                                    onClick = { viewModel.decideExtra(job.id, extra.id, false) },
                                )
                            }
                        }
                    }
                    job.invoice?.let { inv ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Invoice PKR ${inv.total.toInt()} · ${inv.status}",
                            color = MeruTeal,
                            modifier = Modifier.clickable(enabled = !driving) {
                                onOpenInvoice(inv.id)
                            },
                        )
                    }
                    if (job.status == "closed" && !driving) {
                        Spacer(modifier = Modifier.height(8.dp))
                        MeruSecondaryButton(
                            text = "Review 5★",
                            onClick = { viewModel.review(job.id, 5) },
                        )
                    }
                }
            }
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text("Invoices", color = MeruText, fontWeight = FontWeight.SemiBold)
            }
            items(invoices, key = { it.id }) { inv ->
                Text(
                    "${inv.status.uppercase()} · PKR ${inv.total.toInt()}",
                    color = MeruText,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(MeruElevated)
                        .clickable(enabled = !driving) { onOpenInvoice(inv.id) }
                        .padding(14.dp),
                )
            }
        }
    }
}

@Composable
fun InvoiceReviewScreen(
    onBack: () -> Unit,
    onStamped: (ConfirmInvoiceResponse) -> Unit,
    driving: Boolean = false,
    viewModel: InvoiceDetailViewModel = hiltViewModel(),
) {
    val invoice by viewModel.invoice.collectAsState()
    val busy by viewModel.busy.collectAsState()
    val error by viewModel.error.collectAsState()
    LaunchedEffect(Unit) { viewModel.load() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MeruVoid)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
    ) {
        Text(
            "← Back",
            color = MeruTeal,
            modifier = Modifier
                .clickable(onClick = onBack)
                .padding(bottom = 12.dp),
        )
        val inv = invoice
        if (inv == null) {
            Text(error ?: "Loading…", color = MeruMuted)
            return
        }
        Text("Invoice", color = MeruText, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(4.dp))
        Text("Status ${inv.status}", color = MeruCyan)
        Spacer(modifier = Modifier.height(16.dp))
        inv.lines.forEach { line ->
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Text("${line.label} ×${line.qty.toInt()}", color = MeruText, modifier = Modifier.weight(1f))
                Text("PKR ${(line.qty * line.unitPrice).toInt()}", color = MeruMuted)
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text("Total PKR ${inv.total.toInt()}", color = MeruTeal, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Text("PDF stub: ${inv.pdfUrl.take(40)}…", color = MeruMuted, fontSize = 11.sp)
        error?.let {
            Spacer(modifier = Modifier.height(8.dp))
            Text(it, color = MeruAmber)
        }
        Spacer(modifier = Modifier.height(20.dp))
        if (inv.status == "issued") {
            MeruPrimaryButton(
                text = when {
                    driving -> "Locked in Driving Mode"
                    busy -> "Confirming…"
                    else -> "Confirm → stamp vault"
                },
                onClick = {
                    if (!driving && !busy) viewModel.confirmInvoice(onStamped)
                },
            )
            Spacer(modifier = Modifier.height(10.dp))
            MeruSecondaryButton(
                text = "Dispute",
                onClick = { if (!driving) viewModel.dispute() },
            )
        } else {
            Text(
                if (inv.status == "confirmed") "Certified writeback complete." else "Disputed — no writeback.",
                color = MeruMuted,
            )
        }
    }
}

@Composable
fun CertifiedStampScreen(
    result: ConfirmInvoiceResponse,
    onDone: () -> Unit,
) {
    val scale = remember { Animatable(1.6f) }
    val alpha = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        alpha.animateTo(1f, tween(200))
        scale.animateTo(1f, tween(500))
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MeruVoid)
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .graphicsLayer {
                        scaleX = scale.value
                        scaleY = scale.value
                        this.alpha = alpha.value
                        rotationZ = -12f
                    }
                    .border(3.dp, MeruTeal, CircleShape)
                    .clip(CircleShape)
                    .background(MeruTeal.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "CERTIFIED",
                    color = MeruTeal,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                result.writeback?.workshopName ?: "Vault updated",
                color = MeruText,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Service saved into your car’s life.",
                color = MeruMuted,
            )
            Spacer(modifier = Modifier.height(28.dp))
            MeruPrimaryButton(text = "Open garage", onClick = onDone)
        }
    }
}
