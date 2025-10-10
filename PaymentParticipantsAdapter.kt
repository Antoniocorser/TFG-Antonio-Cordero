package Lista.compra.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import Lista.compra.R
import Lista.compra.ShoppingListFragment

class PaymentParticipantsAdapter(
    private var participants: List<ShoppingListFragment.Participant>,
    private var paymentsStatus: Map<String, Boolean>,
    private val onRemoveParticipant: ((ShoppingListFragment.Participant) -> Unit)? = null
) : RecyclerView.Adapter<PaymentParticipantsAdapter.ParticipantViewHolder>() {

    private val currentPaymentsStatus = paymentsStatus.toMutableMap()

    class ParticipantViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val participantName: TextView = itemView.findViewById(R.id.participant_name)
        val participantEmail: TextView = itemView.findViewById(R.id.participant_email)
        val paymentStatus: Button = itemView.findViewById(R.id.payment_status_button)
        val removeButton: Button = itemView.findViewById(R.id.remove_participant_button)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ParticipantViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_payment_participant, parent, false)
        return ParticipantViewHolder(view)
    }

    override fun onBindViewHolder(holder: ParticipantViewHolder, position: Int) {
        val participant = participants[position]
        val isPaid = currentPaymentsStatus[participant.email] ?: false

        // Configurar nombre y email
        holder.participantName.text = participant.name
        holder.participantEmail.text = participant.email

        // Configurar estado de pago
        holder.paymentStatus.text = if (isPaid) "Pagado" else "Pendiente"
        holder.paymentStatus.setBackgroundColor(
            ContextCompat.getColor(
                holder.itemView.context,
                if (isPaid) R.color.green else R.color.red
            )
        )

        // Toggle del estado de pago
        holder.paymentStatus.setOnClickListener {
            currentPaymentsStatus[participant.email] = !isPaid
            notifyItemChanged(position)
        }

        // Configurar botón de eliminar (solo para no propietarios y si hay callback)
        if (onRemoveParticipant != null && !participant.isOwner) {
            holder.removeButton.visibility = View.VISIBLE
            holder.removeButton.setOnClickListener {
                onRemoveParticipant(participant)
            }
        } else {
            holder.removeButton.visibility = View.GONE
        }

        // Si es el propietario, mostrar indicador
        if (participant.isOwner) {
            holder.participantName.text = "${participant.name}"
        }
    }

    override fun getItemCount(): Int = participants.size

    fun getPaymentsStatus(): Map<String, Boolean> = currentPaymentsStatus

    fun updateParticipants(newParticipants: List<ShoppingListFragment.Participant>) {
        participants = newParticipants
        // Mantener solo los estados de pago para participantes que aún existen
        currentPaymentsStatus.keys.retainAll(newParticipants.map { it.email })
        notifyDataSetChanged()
    }

    fun removeParticipant(participant: ShoppingListFragment.Participant) {
        val newParticipants = participants.toMutableList().apply { remove(participant) }
        participants = newParticipants
        currentPaymentsStatus.remove(participant.email)
        notifyDataSetChanged()
    }

    // NUEVO MÉTODO: Obtener participante en una posición específica
    fun getItemAtPosition(position: Int): ShoppingListFragment.Participant {
        return participants[position]
    }

    // NUEVO MÉTODO: Obtener todos los participantes actuales
    fun getCurrentParticipants(): List<ShoppingListFragment.Participant> {
        return participants
    }
}