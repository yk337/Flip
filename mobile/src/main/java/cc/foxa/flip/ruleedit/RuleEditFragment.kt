package cc.foxa.flip.ruleedit

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import dagger.android.support.DaggerFragment
import cc.foxa.flip.cardbrowse.TempFilterListAdapter
import cc.foxa.flip.cardedit.CardEditFragmentArgs
import cc.foxa.flip.databinding.FragmentRuleEditBinding
import cc.foxa.flip.filteredit.KeyWordFilterEditDialog
import cc.foxa.flip.filteredit.PriorityFilterEditDialog
import cc.foxa.flip.filteredit.TagFilterEditDialogFactory
import cc.foxa.flip.filteredit.WithinDaysFilterEditDialog
import cc.foxa.flip.shared.Event
import cc.foxa.flip.shared.EventObserver
import cc.foxa.flip.shared.filter.*
import cc.foxa.flip.util.SpacesItemDecoration
import cc.foxa.flip.util.dip2px
import cc.foxa.flip.util.hideKeyboardFrom
import javax.inject.Inject
import kotlin.reflect.KClass

class RuleEditFragment : DaggerFragment() {
    @Inject lateinit var factory: ViewModelProvider.Factory

    private val viewModel by viewModels<RuleEditViewModel> { factory }

    private lateinit var binding: FragmentRuleEditBinding

    private val args: RuleEditFragmentArgs by navArgs<RuleEditFragmentArgs>()

    private var firstLoadName = true
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requireActivity().onBackPressedDispatcher.addCallback(this) {
            val dialog = RuleEditConfirmDialogFactory.create(this@RuleEditFragment, viewModel)
            if (viewModel.changed) {
                dialog.show()
            } else {
                findNavController().popBackStack()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentRuleEditBinding.inflate(inflater, container, false)
        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setUpFilterList()
        setUpAddFilterSpinner()
        setUpToolbar()
        setUpSnackBar()
        setUpNameInput()
        setUpDeleteAction()
        viewModel.tags.observe(viewLifecycleOwner, Observer {  })
        viewModel.loadRule(args.id)
        viewModel.close.observe(viewLifecycleOwner, EventObserver {
            hideKeyboardFrom(requireContext(), binding.root)
            findNavController().popBackStack()
        })
    }

    private fun setUpDeleteAction() {
        val dialog = MaterialAlertDialogBuilder(context)
            .setTitle("删除此规则？")
            .setPositiveButton("删除") { dialog, _ ->
                viewModel.delete()
                hideKeyboardFrom(requireContext(), binding.root)
                dialog.dismiss()
            }
            .setNegativeButton("取消") { dialog, _ ->
                dialog.dismiss()
            }
            .create()
        binding.delete.setOnClickListener {
            dialog.show()
        }
    }

    private fun setUpNameInput() {
        binding.nameInput.addTextChangedListener {
            if (firstLoadName) {
                firstLoadName = false
            } else {
                viewModel.changed = true
            }
        }
    }

    private fun setUpSnackBar() {
        viewModel.snackbar.observe(viewLifecycleOwner, EventObserver {
            Snackbar.make(binding.root, it, Snackbar.LENGTH_SHORT).show()
        })
    }

    private fun setUpToolbar() {
        binding.modeToggle.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (checkedId == binding.combo.id) {
                if (isChecked) {
                    binding.next.visibility = View.VISIBLE
                    binding.save.visibility = View.GONE
                } else {
                    binding.next.visibility = View.GONE
                    binding.save.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun setUpFilterList() {
        if (binding.filterList.adapter == null) {
            binding.filterList.adapter = FilterListAdapter(viewModel)
            binding.filterList.addItemDecoration(
                SpacesItemDecoration(dip2px(requireContext(), 4F).toInt()))
        }
        viewModel.refreshFilterList.observe(viewLifecycleOwner, Observer {
            (binding.filterList.adapter as FilterListAdapter).apply {
                submitList(viewModel.rule.value?.filters)
            }
        })
        viewModel.refreshFilterList.value = Event(Unit)
    }


    private fun setUpAddFilterSpinner() {
        binding.addFilter.apply {
            setItems(FILTER_NAME.values.toList())
            setOnItemSelectedListener { _, position, _, _ ->
                showAddFilterDialog(FILTER_NAME.keys.toList()[position])
            }
        }
    }

    private fun showAddFilterDialog(klass: KClass<out Filter>) {
        when (klass) {
            TagFilter::class -> {
                TagFilterEditDialogFactory.create(requireContext(), viewModel.tags.value!!) {
                    viewModel.rule.value!!.filters += (TagFilter(tags = it))
                    viewModel.changed = true
                    (binding.filterList.adapter as FilterListAdapter).apply {
                        submitList(viewModel.rule.value!!.filters)
                        notifyDataSetChanged()
                    }
                }.show()
            }
            KeyWordFilter::class -> {
                KeyWordFilterEditDialog() {
                    viewModel.rule.value!!.filters += (KeyWordFilter(keyword = it))
                    viewModel.changed = true
                    (binding.filterList.adapter as FilterListAdapter).apply {
                        submitList(viewModel.rule.value!!.filters)
                        notifyDataSetChanged()
                    }
                }.show(parentFragmentManager, null)
            }
            PriorityFilter::class -> {
                PriorityFilterEditDialog { from, to ->
                    viewModel.rule.value!!.filters += (PriorityFilter(fromPriority = from, toPriority = to))
                    viewModel.changed = true
                    (binding.filterList.adapter as FilterListAdapter).apply {
                        submitList(viewModel.rule.value!!.filters)
                        notifyDataSetChanged()
                    }
                }.show(parentFragmentManager, null)
            }
            WithinDaysFilter::class -> {
                WithinDaysFilterEditDialog { day ->
                    viewModel.rule.value!!.filters += (WithinDaysFilter(days = day))
                    viewModel.changed = true
                    (binding.filterList.adapter as FilterListAdapter).apply {
                        submitList(viewModel.rule.value!!.filters)
                        notifyDataSetChanged()
                    }
                }.show(parentFragmentManager, null)
            }
        }
    }
}