package Lista.compra.adapters

import Lista.compra.R
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import Lista.compra.ShoppingListFragment

class ExcludedParticipantsAdapter(
    private val excludedParticipants: List<ShoppingListFragment.Participant>,
    private val onAddParticipant: (String) -> Unit
) : RecyclerView.Adapter<ExcludedParticipantsAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nameText: TextView = itemView.findViewById(R.id.excluded_participant_name)
        private val emailText: TextView = itemView.findViewById(R.id.excluded_participant_email)
        private val addButton: Button = itemView.findViewById(R.id.add_participant_button)

        fun bind(participant: ShoppingListFragment.Participant) {
            nameText.text = participant.name
            emailText.text = participant.email

            addButton.setOnClickListener {
                onAddParticipant(participant.email)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_excluded_participant, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(excludedParticipants[position])
    }

    override fun getItemCount(): Int = excludedParticipants.size
}