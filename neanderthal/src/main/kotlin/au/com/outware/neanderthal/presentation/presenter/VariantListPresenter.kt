package au.com.outware.neanderthal.presentation.presenter

import android.os.Bundle
import au.com.outware.neanderthal.Neanderthal
import au.com.outware.neanderthal.data.model.Variant
import java.util.*

/**
 * @author timmutton
 */
class VariantListPresenter constructor(val view: ViewSurface,
                                               val adapter: AdapterSurface): Presenter {
    companion object {
        val CURRENT_POSITION_KEY = "current_position"
        val CURRENT_VARIANT_NAME_KEY = "current_variant_name"
        val VARIANTS_KEY = "variants"
    }

    private var variants: ArrayList<String> = ArrayList<String>()
    private var currentVariantName: String? = null
    private var currentPosition = 0

    private var deletedVariant: Variant? = null

    // region Lifecycle
    override fun onCreate(parameters: Bundle?) {
        if(parameters != null) {
            currentPosition = parameters.getInt(CURRENT_POSITION_KEY)
            currentVariantName = parameters.getString(CURRENT_VARIANT_NAME_KEY)
            variants.addAll(parameters.getStringArrayList(VARIANTS_KEY) ?: emptyList<String>())
            if(variants.isNotEmpty()) {
                variants.sort()
                adapter.setCurrentPosition(currentPosition)
            }
        } else {
            variants.addAll(getVariantNames())
            if(variants.isNotEmpty()) {
                currentVariantName = Neanderthal.variantRepository?.getCurrentVariant()?.name ?: variants.first()
                currentPosition = variants.indexOf(currentVariantName!!)
                variants.sort()
                adapter.setCurrentPosition(variants.indexOf(currentVariantName!!))
            }
        }

        adapter.add(variants)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        if(variants.isNotEmpty()) {
            outState.putInt(CURRENT_POSITION_KEY, currentPosition)
            outState.putString(CURRENT_VARIANT_NAME_KEY, currentVariantName)
            outState.putStringArrayList(VARIANTS_KEY, ArrayList<String>(variants))
        }
    }

    override fun onPause() {
        view.dismissDeleteConfirmation()
    }
    // endregion

    fun onItemSelected(name: String, position: Int) {
        Neanderthal.variantRepository?.setCurrentVariant(name)
        currentVariantName = name
        currentPosition = position
        adapter.setCurrentPosition(position)
    }

    fun onAddClicked() {
        view.goToAddVariant()
    }

    fun onEditClicked() {
        view.goToEditVariant(currentVariantName!!)
    }

    fun onDeleteClicked() {
        view.createDeleteConfirmation()
    }

    fun onAddVariant(name: String) {
        currentVariantName = name
        variants.add(name)
        variants.sort()
        adapter.add(variants)

        updateEditingEnabled()
    }

    fun onResetToDefaultClicked(){
        view.createResetConfirmation()
    }

    fun onResetConfirmation(confirmed: Boolean){
        if(confirmed) {
            Neanderthal.variantRepository?.resetVariants()
            variants.clear()

            variants.addAll(getVariantNames())

            variants.sort()
            adapter.add(variants)
            currentVariantName = Neanderthal.variantRepository?.getCurrentVariant()?.name ?: variants.firstOrNull()
            currentPosition = variants.indexOf(currentVariantName)
            adapter.setCurrentPosition(currentPosition)
            view.notifyReset()
        }

        view.dismissResetConfirmation()
        updateEditingEnabled()
    }

    fun onDeleteConfirmation(confirmed: Boolean) {
        if (confirmed) {
            deletedVariant = Neanderthal.variantRepository?.getCurrentVariant()

            val oldPosition = currentPosition
            Neanderthal.variantRepository?.removeVariant(currentVariantName!!)
            variants.remove(currentVariantName!!)

            if(variants.isNotEmpty()) {
                // Set new current variant
                currentPosition = Math.max(--currentPosition, 0)
                currentVariantName = variants[currentPosition]
                Neanderthal.variantRepository?.setCurrentVariant(currentVariantName!!)
                adapter.setCurrentPosition(currentPosition)
            }else{
                currentPosition = 0
                currentVariantName = ""
            }

            // Notify the views
            adapter.remove(oldPosition)
            view.notifyDeleted()
        }

        view.dismissDeleteConfirmation()
        updateEditingEnabled()
    }

    fun onUndoClicked() {
        deletedVariant?.let {
            val name = deletedVariant!!.name

            if(deletedVariant!!.name == null) {
                throw IllegalArgumentException("Added or updated variant must have a name")
            }

            Neanderthal.variantRepository?.addVariant(deletedVariant!!)
            Neanderthal.variantRepository?.setCurrentVariant(name!!)

            variants.add(name!!)
            adapter.add(name)
        }
    }

    fun onLaunchClicked() {
        view.goToMainApplication()
    }

    fun updateEditingEnabled() {
        view.setEditingEnabled(variants.size != 0)
    }

    fun getVariantNames(): List<String> {
        Neanderthal.variantRepository?.let {
            return it.getVariants().map { variant -> variant.name!! }.toList()
        }
        return emptyList<String>()
    }

    interface ViewSurface {
        fun createDeleteConfirmation()
        fun dismissDeleteConfirmation()
        fun notifyDeleted()
        fun createResetConfirmation()
        fun dismissResetConfirmation()
        fun notifyReset()
        fun goToAddVariant()
        fun goToEditVariant(name: String)
        fun goToMainApplication()
        fun setEditingEnabled(enabled : Boolean)
    }

    interface AdapterSurface {
        fun setCurrentPosition(position: Int)
        fun add(variants: List<String>)
        fun add(variant: String)
        fun remove(index: Int)
    }
}